package com.kia.blagogramapp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kia.blagogramapp.api.ApiRepository
import com.kia.blagogramapp.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    fun loadMessages(token: String, chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val result = repo.getMessages(token, chatId)
                _messages.postValue(result)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Unknown error")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun sendMessage(token: String, chatId: Int, text: String, files: List<File>?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.sendMessage(token, chatId, text, files)
                _isMessageSent.postValue(true)
                loadMessages(token, chatId)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to send message")
            }
        }
    }

    fun deleteMessage(token: String, messageId: Int, chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.deleteMessage(token, messageId)
                loadMessages(token, chatId)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to delete message")
            }
        }
    }

    fun editMessage(token: String, messageId: Int, text: String, chatId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.editMessage(token, messageId, text)
                loadMessages(token, chatId)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to edit message")
            }
        }
    }
}
