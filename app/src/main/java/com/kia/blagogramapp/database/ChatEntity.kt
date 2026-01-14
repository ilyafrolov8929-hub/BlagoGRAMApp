package com.kia.blagogramapp.database
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: Int,
    val name: String?,
    val avatar: String?,
    val lastMsg: String?,
    val lastTime: String?,
    val unread: Int,
    val lastSenderId: Int?,
    val userId: Int?,
    val pinned: Boolean,
    val isPremium: Boolean
)