package com.kia.blagogramapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.database.AppDatabase
import com.kia.blagogramapp.database.MessageEntity
import com.kia.blagogramapp.model.Message
import kotlinx.coroutines.*
import java.io.File

class ChatViewModel(application: Application, private val repo: ApiRepository) : AndroidViewModel(application) {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isMessageSent = MutableLiveData<Boolean>()
    val isMessageSent: LiveData<Boolean> = _isMessageSent

    private var pollingJob: Job? = null
    private var statusJob: Job? = null
    
    private var lastMessageId: Int = 0
    private var currentChatId: Int = -1
    private var currentToken: String = ""
    
    private val db = AppDatabase.getDatabase(application)
    private val gson = Gson()

    fun loadMessages(token: String, chatId: Int) {
        currentToken = token
        currentChatId = chatId
        
        // 1. Загружаем из кэша
        viewModelScope.launch(Dispatchers.IO) {
            val cached = db.messageDao().getByChatId(chatId)
            if (cached.isNotEmpty()) {
                val mapped = cached.map {
                    Message(it.id, it.chatId, it.senderId, it.content, it.sentAt, it.editedAt, it.isRead, it.displayName, it.avatar, gson.fromJson(it.images, Array<String>::class.java).toList(), it.isPremium)
                }
                _messages.postValue(mapped)
                lastMessageId = mapped.maxOf { it.id }
            }
            
            // 2. Идем в сеть
            fetchFromNetwork(token, chatId)
        }
    }

    private suspend fun fetchFromNetwork(token: String, chatId: Int) {
        try {
            repo.markAsRead(token, chatId)
            val result = repo.getMessages(token, chatId)
            _messages.postValue(result)
            
            if (result.isNotEmpty()) {
                lastMessageId = result.maxOf { it.id }
                
                // Сохраняем в кэш
                db.messageDao().deleteByChatId(chatId)
                db.messageDao().insertAll(result.map {
                    MessageEntity(it.id, it.chat_id, it.sender_id, it.content, it.sent_at, it.edited_at, it.is_read, it.display_name, it.avatar, gson.toJson(it.images), it.is_premium)
                })
            }
            startRealtime()
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Unknown error")
        }
    }

    private fun startRealtime() {
        pollingJob?.cancel()
        statusJob?.cancel()

        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val newMessages = repo.getMessagesLong(currentToken, currentChatId, lastMessageId)
                    if (newMessages.isNotEmpty()) {
                        repo.markAsRead(currentToken, currentChatId)
                        val updatedList = _messages.value.orEmpty().toMutableList()
                        val newFiltered = newMessages.filter { nm -> updatedList.none { it.id == nm.id } }
                        if (newFiltered.isNotEmpty()) {
                            updatedList.addAll(newFiltered)
                            updatedList.sortBy { it.id }
                            _messages.postValue(updatedList)
                            lastMessageId = updatedList.maxOf { it.id }
                            
                            // Обновляем кэш
                            db.messageDao().insertAll(newFiltered.map {
                                MessageEntity(it.id, it.chat_id, it.sender_id, it.content, it.sent_at, it.edited_at, it.is_read, it.display_name, it.avatar, gson.toJson(it.images), it.is_premium)
                            })
                        }
                    }
                } catch (e: Exception) {
                    delay(3000)
                }
                delay(500)
            }
        }

        statusJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(4000)
                try {
                    val latest = repo.getMessages(currentToken, currentChatId)
                    val current = _messages.value.orEmpty()
                    
                    val statusChanged = latest.size != current.size || latest.any { l -> 
                        current.find { it.id == l.id }?.is_read != l.is_read ||
                        current.find { it.id == l.id }?.content != l.content ||
                        current.find { it.id == l.id }?.edited_at != l.edited_at
                    }

                    if (statusChanged) {
                        _messages.postValue(latest)
                        // Обновляем кэш при изменении статусов
                        db.messageDao().deleteByChatId(currentChatId)
                        db.messageDao().insertAll(latest.map {
                            MessageEntity(it.id, it.chat_id, it.sender_id, it.content, it.sent_at, it.edited_at, it.is_read, it.display_name, it.avatar, gson.toJson(it.images), it.is_premium)
                        })
                    }
                } catch (e: Exception) { }
            }
        }
    }

    fun addTempMessage(message: Message) {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(message)
        _messages.postValue(current)
    }

    fun sendMessage(token: String, chatId: Int, text: String, files: List<File>?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = _messages.value.orEmpty().toMutableList()
                current.forEachIndexed { index, message ->
                    if (message.is_read == 0) current[index] = message.copy(is_read = 1)
                }
                _messages.postValue(current)

                repo.sendMessage(token, chatId, text, files)
                repo.markAsRead(token, chatId)
                _isMessageSent.postValue(true)
                
                fetchFromNetwork(token, chatId)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to send message")
            }
        }
    }

    fun editMessage(token: String, messageId: Int, text: String, chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.editMessage(token, messageId, text)
                fetchFromNetwork(token, chatId)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to edit message")
            }
        }
    }

    fun deleteMessage(token: String, messageId: Int, chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.deleteMessage(token, messageId)
                fetchFromNetwork(token, chatId)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to delete message")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        statusJob?.cancel()
    }
}
