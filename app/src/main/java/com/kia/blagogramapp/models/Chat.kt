package com.kia.blagogramapp.model

data class Chat(
    val id: Int,
    val name: String?,
    val avatar: String?,
    val last_msg: String?,
    val last_time: String?,
    val unread: Int,
    val last_sender_id: Int?
)

