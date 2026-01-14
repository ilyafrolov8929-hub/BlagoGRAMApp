package com.kia.blagogramapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Int,
    val chatId: Int,
    val senderId: Int,
    val content: String,
    val sentAt: String,
    val editedAt: String?,
    val isRead: Int,
    val displayName: String,
    val avatar: String,
    val images: String, // Храним как JSON строку или разделитель
    val isPremium: Boolean
)
