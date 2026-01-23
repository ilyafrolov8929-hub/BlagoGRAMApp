package com.kia.blagogramapp.models

import com.kia.blagogramapp.model.UserProfile

data class ChatInfoResponse(
    val chat_id: Int,
    val created_at: String,
    val interlocutor: UserProfile?,
    val statistics: ChatStatistics,
    val recent_photos: List<RecentPhoto>,
    val recent_link_messages: List<RecentLinkMessage>
)

data class ChatStatistics(
    val total_messages: Int,
    val text_messages: Int,
    val media_messages: Int,
    val links_messages: Int,
    val photos_count: Int
)

data class RecentPhoto(
    val message_id: Int,
    val filename: String,
    val sent_at: String
)

data class RecentLinkMessage(
    val message_id: Int,
    val sender_id: Int,
    val sender_name: String,
    val sender_avatar: String?,
    val content_preview: String,
    val sent_at: String
)
