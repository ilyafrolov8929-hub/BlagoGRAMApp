package com.kia.blagogramapp.ui

import android.app.Application
import android.util.Log
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

    private val TAG = "ChatViewModel"
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
        
        viewModelScope.launch(Dispatchers.IO) {
            val cached = db.messageDao().getByChatId(chatId)
            if (cached.isNotEmpty()) {
                val mapped = cached.map {
                    Message(
                        id = it.id,
                        chat_id = it.chatId,
                        sender_id = it.senderId,
                        content = it.content,
                        sent_at = it.sentAt,
                        edited_at = it.editedAt,
                        is_read = it.isRead,
                        display_name = it.displayName,
                        avatar = it.avatar,
                        images = try { gson.fromJson(it.images, Array<String>::class.java).toList() } catch (e: Exception) { emptyList() },
                        is_premium = it.isPremium,
                        reply = if (it.replyId != null) com.kia.blagogramapp.model.ReplyMessage(
                            id = it.replyId,
                            sender_id = it.replySenderId ?: 0,
                            display_name = it.replyDisplayName ?: "",
                            content = it.replyContent ?: "",
                            images = try { gson.fromJson(it.replyImages, Array<String>::class.java).toList() } catch (e: Exception) { emptyList() }
                        ) else null
                    )
                }
                _messages.postValue(mapped.sortedBy { it.sent_at })
                lastMessageId = mapped.filter { it.id > 0 }.maxOfOrNull { it.id } ?: 0
            }
            fetchFromNetwork(token, chatId)
        }
    }

    private suspend fun fetchFromNetwork(token: String, chatId: Int) {
        try {
            // Временно отключаем markAsRead, так как бэкенд возвращает 400 Unknown action
            // repo.markAsRead(token, chatId)
            
            val result = repo.getMessages(token, chatId)
            
            val current = _messages.value.orEmpty()
            
            val tempMessages = current.filter { temp -> 
                temp.id < 0 && result.none { real -> real.sender_id == temp.sender_id && real.content == temp.content }
            }
            
            val newList = (result + tempMessages).sortedBy { it.sent_at }
            _messages.postValue(newList)
            
            if (result.isNotEmpty()) {
                lastMessageId = result.maxOf { it.id }
                db.messageDao().deleteByChatId(chatId)
                db.messageDao().insertAll(result.map {
                    MessageEntity(
                        id = it.id,
                        chatId = it.chat_id,
                        senderId = it.sender_id,
                        content = it.content,
                        sentAt = it.sent_at,
                        editedAt = it.edited_at,
                        isRead = it.is_read,
                        displayName = it.display_name,
                        avatar = it.avatar,
                        images = gson.toJson(it.images),
                        isPremium = it.is_premium,
                        replyId = it.reply?.id,
                        replySenderId = it.reply?.sender_id,
                        replyDisplayName = it.reply?.display_name,
                        replyContent = it.reply?.content,
                        replyImages = gson.toJson(it.reply?.images ?: emptyList<String>())
                    )
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
                        // repo.markAsRead(currentToken, currentChatId)
                        val updatedList = _messages.value.orEmpty().toMutableList()
                        val myId = (getApplication() as Application).getSharedPreferences("app", android.content.Context.MODE_PRIVATE).getInt("user_id", 0)

                        newMessages.forEach { nm ->
                            if (nm.sender_id == myId) {
                                updatedList.removeAll { it.id < 0 && it.content == nm.content }
                            }
                        }

                        val newFiltered = newMessages.filter { nm -> updatedList.none { it.id == nm.id } }
                        if (newFiltered.isNotEmpty()) {
                            updatedList.addAll(newFiltered)
                            updatedList.sortBy { it.sent_at }
                            _messages.postValue(updatedList)
                            lastMessageId = updatedList.filter { it.id > 0 }.maxOfOrNull { it.id } ?: lastMessageId
                            
                            db.messageDao().insertAll(newFiltered.map {
                                MessageEntity(it.id, it.chat_id, it.sender_id, it.content, it.sent_at, it.edited_at, it.is_read, it.display_name, it.avatar, gson.toJson(it.images), it.is_premium, it.reply?.id, it.reply?.sender_id, it.reply?.display_name, it.reply?.content, gson.toJson(it.reply?.images ?: emptyList<String>()))
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
                    
                    val tempMessages = current.filter { temp -> 
                        temp.id < 0 && latest.none { real -> real.sender_id == temp.sender_id && real.content == temp.content }
                    }
                    
                    val statusChanged = latest.size != current.filter { it.id > 0 }.size || latest.any { l -> 
                        current.find { it.id == l.id }?.is_read != l.is_read ||
                        current.find { it.id == l.id }?.content != l.content ||
                        current.find { it.id == l.id }?.edited_at != l.edited_at
                    }

                    if (statusChanged || tempMessages.size != current.filter { it.id < 0 }.size) {
                        _messages.postValue((latest + tempMessages).sortedBy { it.sent_at })
                        db.messageDao().deleteByChatId(currentChatId)
                        db.messageDao().insertAll(latest.map {
                            MessageEntity(it.id, it.chat_id, it.sender_id, it.content, it.sent_at, it.edited_at, it.is_read, it.display_name, it.avatar, gson.toJson(it.images), it.is_premium, it.reply?.id, it.reply?.sender_id, it.reply?.display_name, it.reply?.content, gson.toJson(it.reply?.images ?: emptyList<String>()))
                        })
                    }
                } catch (e: Exception) { }
            }
        }
    }

    fun addTempMessage(message: Message) {
        val current = _messages.value.orEmpty().toMutableList()
        current.add(message)
        current.sortBy { it.sent_at }
        _messages.postValue(current)
    }

    fun sendMessage(token: String, chatId: Int, text: String, files: List<File>?, replyToId: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.sendMessage(token, chatId, text, files, replyToId)
                // repo.markAsRead(token, chatId)
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
