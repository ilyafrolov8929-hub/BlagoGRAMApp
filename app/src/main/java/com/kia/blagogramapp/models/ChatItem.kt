package com.kia.blagogramapp.models

data class ChatItem(
    val chat_id: Int,
    val user_id: Int,
    val name: String,
    val avatar: String?,
    val last_message: String?,
    val last_time: String?,
    val unread: Int
)

