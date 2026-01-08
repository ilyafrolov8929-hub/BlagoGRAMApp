package com.kia.blagogramapp.utils

import android.content.Context
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.model.UserProfile
import com.kia.blagogramapp.models.SearchUserItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object DevModeManager {
    private const val PREFS_NAME = "dev_mode_prefs"
    private const val KEY_IS_ENABLED = "is_dev_mode_enabled"

    private val mockMessages = mutableListOf<Message>()
    private var isInitialized = false

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ENABLED, enabled)
            .apply()
    }

    private fun initDataIfNeeded() {
        if (isInitialized) return
        val now = getCurrentTimestamp()
        
        // Чат 2: Тестовый юзер
        mockMessages.add(createMsg(1, 2, 2, "Привет! Как тебе новый дизайн?", now, "Тестовый Юзер"))
        
        // Чат 1: Избранное (ID нашего пользователя = 1)
        mockMessages.add(createMsg(2, 1, 1, "Это мое облако для заметок ☁️", now, "Разработчик"))
        
        isInitialized = true
    }

    private fun createMsg(id: Int, chatId: Int, senderId: Int, text: String, time: String, name: String, images: List<String> = emptyList()): Message {
        return Message(id, chatId, senderId, text, time, null, 1, "", name, images)
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun getMockUserProfile(userId: Int = 1): UserProfile {
        return if (userId == 2) {
            UserProfile(2, "test_user", "Тестовый Юзер", null, "Я тоже премиум!", 5, 10, true)
        } else {
            UserProfile(1, "dev_user", "Разработчик (Local)", null, "Режим разработки", 7, 77, true)
        }
    }

    fun getMockChats(): List<Chat> {
        val now = getCurrentTimestamp()
        return listOf(
            // Чат с самим собой = Избранное
            Chat(1, "Избранное", "action_saved", "Мои заметки", now, 0, 1, 1, true),
            // Обычный чат
            Chat(2, "Тестовый Юзер", null, "Привет! Как тебе...", now, 1, 2, 2, false)
        )
    }

    fun getMockMessages(chatId: Int): List<Message> {
        initDataIfNeeded()
        return mockMessages.filter { it.chat_id == chatId }
    }

    fun addMockMessage(chatId: Int, text: String, images: List<File>? = null) {
        initDataIfNeeded()
        val nextId = (mockMessages.maxOfOrNull { it.id } ?: 0) + 1
        // Добавляем префикс file:// чтобы адаптер не искал фото в интернете
        val imagePaths = images?.map { "file://${it.absolutePath}" } ?: emptyList()
        mockMessages.add(createMsg(nextId, chatId, 1, text, getCurrentTimestamp(), "Разработчик", imagePaths))
    }

    fun getMockSearchUsers(query: String): List<SearchUserItem> {
        return listOf(
            SearchUserItem(1, "dev_user", "Разработчик (Я)", null, true),
            SearchUserItem(2, "test_user", "Тестовый Юзер", null, true)
        ).filter { it.display_name.contains(query, ignoreCase = true) }
    }
}
