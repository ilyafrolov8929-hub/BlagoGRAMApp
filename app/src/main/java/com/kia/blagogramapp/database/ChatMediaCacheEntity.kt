package com.kia.blagogramapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_media_cache")
data class ChatMediaCacheEntity(
    @PrimaryKey val chatId: Int,
    val jsonResponse: String, // Храним весь JSON ответа chat.getInfo для простоты
    val updatedAt: Long = System.currentTimeMillis()
)
