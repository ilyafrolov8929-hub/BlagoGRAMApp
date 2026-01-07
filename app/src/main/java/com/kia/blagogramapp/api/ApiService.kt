package com.kia.blagogramapp.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @FormUrlEncoded
    @POST("index.php")
    suspend fun login(
        @Field("action") action: String = "auth.login",
        @Field("login") login: String,
        @Field("password") password: String
    ): LoginResponse

    @FormUrlEncoded
    @POST("index.php")
    suspend fun getChats(
        @Field("action") action: String = "chats.get",
        @Field("token") token: String
    ): ChatListResponse


    @FormUrlEncoded
    @POST("index.php")
    suspend fun getMessages(
        @Field("action") action: String = "messages.get",
        @Field("token") token: String,
        @Field("chat_id") chatId: Int
    ): MessageListResponse


    @Multipart
    @POST("index.php")
    suspend fun sendMessage(
        @Part("action") action: RequestBody,
        @Part("token") token: RequestBody,
        @Part("chat_id") chatId: RequestBody,
        @Part("text") text: RequestBody?,
        @Part image: List<MultipartBody.Part>?
    ): SendMessageResponse

    @FormUrlEncoded
    @POST("index.php")
    suspend fun editMessage(
        @Field("action") action: String = "messages.edit",
        @Field("token") token: String,
        @Field("message_id") messageId: Int,
        @Field("text") text: String
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("index.php")
    suspend fun deleteMessage(
        @Field("action") action: String = "messages.delete",
        @Field("token") token: String,
        @Field("message_id") messageId: Int
    ): Response<Map<String, Any>>


    @FormUrlEncoded
    @POST("index.php")
    suspend fun createChat(
        @Field("action") action: String = "chat.create",
        @Field("token") token: String,
        @Field("user_id") userId: Int
    ): CreateChatResponse

    @FormUrlEncoded
    @POST("index.php")
    suspend fun deleteChat(
        @Field("action") action: String = "chat.delete",
        @Field("token") token: String,
        @Field("chat_id") chatId: Int,
        @Field("mode") mode: String
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("index.php")
    suspend fun pinChat(
        @Field("action") action: String = "chat.pin",
        @Field("token") token: String,
        @Field("chat_id") chatId: Int
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("index.php")
    suspend fun unpinChat(
        @Field("action") action: String = "chat.unpin",
        @Field("token") token: String,
        @Field("chat_id") chatId: Int
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("get_chats.php")
    suspend fun getChats(
        @Field("user_id") userId: Int
    ): ChatsResponse

    @Multipart
    @POST("index.php")
    suspend fun profileEditMultipart(
        @Part("action") action: RequestBody,
        @Part("token") token: RequestBody,
        @Part("display_name") displayName: RequestBody?,
        @Part("status") status: RequestBody?,
        @Part avatar: MultipartBody.Part?
    ): Response<Map<String, Any>>

    @FormUrlEncoded
    @POST("index.php")
    suspend fun checkSubscription(
        @Field("action") action: String = "subscription.check",
        @Field("token") token: String
    ): SubscriptionCheckResponse

    @FormUrlEncoded
    @POST("index.php")
    suspend fun buySubscription(
        @Field("action") action: String = "subscription.buy",
        @Field("token") token: String,
        @Field("plan") plan: String // 'month', 'year', 'lifetime'
    ): SubscriptionBuyResponse
}
