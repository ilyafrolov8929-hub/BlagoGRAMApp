package com.kia.blagogramapp.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Message
import kotlinx.coroutines.*
import java.io.File

class ChatViewModel(private val repo: ApiRepository) : ViewModel() {

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

    fun loadMessages(token: String, chatId: Int) {
        currentToken = token
        currentChatId = chatId
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                repo.markAsRead(token, chatId)
                val result = repo.getMessages(token, chatId)
                _messages.postValue(result)
                if (result.isNotEmpty()) {
                    lastMessageId = result.maxOf { it.id }
                }
                startRealtime()
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Unknown error")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun startRealtime() {
        pollingJob?.cancel()
        statusJob?.cancel()

        // 1. Получение НОВЫХ сообщений (Long Polling)
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
                        }
                    }
                } catch (e: Exception) {
                    delay(3000)
                }
                delay(500)
            }
        }

        // 2. Обновление СТАТУСОВ (Галочки)
        statusJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(4000) // Проверяем статусы каждые 4 секунды
                try {
                    val latest = repo.getMessages(currentToken, currentChatId)
                    val current = _messages.value.orEmpty()
                    
                    // Проверяем, изменились ли статусы is_read
                    val statusChanged = latest.size != current.size || latest.any { l -> 
                        current.find { it.id == l.id }?.is_read != l.is_read ||
                        current.find { it.id == l.id }?.content != l.content
                    }

                    if (statusChanged) {
                        _messages.postValue(latest)
                    }
                } catch (e: Exception) { }
            }
        }
    }

    fun sendMessage(token: String, chatId: Int, text: String, files: List<File>?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Перед отправкой помечаем всё как прочитанное локально, чтобы убрать хедер
                val current = _messages.value.orEmpty().toMutableList()
                current.forEachIndexed { index, message ->
                    if (message.is_read == 0) current[index] = message.copy(is_read = 1)
                }
                _messages.postValue(current)

                repo.sendMessage(token, chatId, text, files)
                repo.markAsRead(token, chatId)
                _isMessageSent.postValue(true)
                
                val updated = repo.getMessages(token, chatId)
                _messages.postValue(updated)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to send message")
            }
        }
    }

    fun editMessage(token: String, messageId: Int, text: String, chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.editMessage(token, messageId, text)
                val result = repo.getMessages(token, chatId)
                _messages.postValue(result)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to edit message")
            }
        }
    }

    fun deleteMessage(token: String, messageId: Int, chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.deleteMessage(token, messageId)
                val result = repo.getMessages(token, chatId)
                _messages.postValue(result)
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
