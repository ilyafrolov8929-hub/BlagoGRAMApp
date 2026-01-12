package com.kia.blagogramapp.model

data class Message(
    val id: Int,
    val chat_id: Int,
    val sender_id: Int,
    val content: String,
    val sent_at: String,
    val edited_at: String? = null,
    val is_read: Int,
    val display_name: String,
    val avatar: String,
    val images: List<String> = emptyList(),
    val is_premium: Boolean = false
)
