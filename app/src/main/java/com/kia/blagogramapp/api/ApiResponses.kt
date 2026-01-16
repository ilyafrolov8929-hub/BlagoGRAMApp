package com.kia.blagogramapp.api

import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.model.Post
import com.kia.blagogramapp.models.SearchUserItem


data class LoginResponse(
    val ok: Boolean,
    val result: LoginResult?,
    val error: String?
)

data class LoginResult(
    val user_id: Int?,
    val username: String?,
    val phone: String?,
    val display_name: String?,
    val avatar: String?,
    val cover: String?,
    val token: String?,
    val status: String?,
    val deep_link: String?,
    val verify_token: String?
)

data class VerificationResponse(
    val ok: Boolean,
    val status: String? = null,    // pending / success / error
    val result: LoginResult? = null,
    val error: String? = null,
    val message: String? = null
)

data class ChatsResponse(
    val status: Boolean,
    val chats: List<Chat>
)

data class ChatListResponse(
    val ok: Boolean,
    val result: List<ChatItem>?,
    val error: String?
)

data class ChatItem(
    val chat_id: Int,
    val user_id: Int,
    val name: String,
    val avatar: String,
    val last_message: String?,
    val last_time: String?,
    val unread: Int,
    val is_premium: Boolean = false
)

data class MessageListResponse(
    val ok: Boolean,
    val result: List<MessageItem>?,
    val error: String?
)

data class MessageItem(
    val id: Int,
    val chat_id: Int,
    val sender_id: Int,
    val content: String,
    val sent_at: String,
    val edited_at: String?,
    val is_read: Int,
    val display_name: String,
    val avatar: String,
    val images: List<String>?,
    val is_premium: Boolean = false
)

data class SendMessageResponse(
    val ok: Boolean,
    val result: SendMessageResult?,
    val error: String?
)

data class SendMessageResult(
    val message_id: Int,
    val sent_at: String,
    val images: List<String>?,
    val text: String?
)

data class CreateChatResponse(
    val ok: Boolean,
    val result: CreateChatResult?,
    val error: String?
)

data class CreateChatResult(
    val chat_id: Int
)

data class SearchUsersResponse(
    val ok: Boolean,
    val result: List<SearchUserItem>?,
    val error: String?
)

data class SearchUsersResult(
    val users: List<SearchUserItem>
)

data class SubscriptionCheckResponse(
    val ok: Boolean,
    val result: SubscriptionCheckResult?,
    val error: String?
)

data class SubscriptionCheckResult(
    val active: Any?, // Изменили на Any, чтобы принять и Boolean, и Int
    val plan: String?,
    val expires_at: String?
)

data class SubscriptionBuyResponse(
    val ok: Boolean,
    val result: SubscriptionBuyResult?,
    val error: String?
)

data class SubscriptionBuyResult(
    val message: String,
    val plan: String,
    val expires_at: String?
)

data class DatabaseDumpResponse(
    val ok: Boolean,
    val result: DatabaseDumpResult?,
    val error: String?
)

data class DatabaseDumpResult(
    val dump: Map<String, List<Map<String, Any>>>?,
    val tables_count: Int,
    val generated_at: String,
    val warning: String
)

data class PostListResponse(
    val ok: Boolean,
    val result: List<Post>?,
    val error: String?
)

data class UserPostsResponse(
    val ok: Boolean,
    val result: UserPostsResult?,
    val error: String?
)

data class UserPostsResult(
    val posts: List<Post>?,
    val total_count: Int,
    val has_more: Boolean
)
