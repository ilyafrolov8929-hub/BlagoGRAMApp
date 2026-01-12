package com.kia.blagogramapp.api

import android.util.Log
import com.kia.blagogramapp.BlagoApi
import com.kia.blagogramapp.DesignEngine
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.model.UserProfile
import com.kia.blagogramapp.model.PremiumInfo
import com.kia.blagogramapp.models.SearchUserItem
import com.kia.blagogramapp.utils.DevModeManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ApiRepository(private val api: BlagoApi) {

    private val TAG = "ApiRepository"

    private fun isDevMode(): Boolean {
        return DesignEngine.context?.let { DevModeManager.isEnabled(it) } ?: false
    }

    // -------------------------------------------------
    // LOGIN
    // -------------------------------------------------
    suspend fun login(login: String, password: String): Map<String, Any> {
        if (isDevMode()) {
            @Suppress("UNCHECKED_CAST")
            return mapOf(
                "token" to "mock_token",
                "user_id" to 1,
                "username" to "dev_user",
                "display_name" to "Разработчик (Local)",
                "status" to "Режим разработки активен",
                "avatar" to "",
                "cover" to ""
            ) as Map<String, Any>
        }

        val resp = api.call(
            ApiRequest(
                action = "auth.login",
                login = login,
                password = password
            )
        )

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string()
            Log.e(TAG, "login error: ${resp.code()} $errorBody")
            throw Exception("API Error: ${resp.code()} $errorBody")
        }

        val body = resp.body() ?: throw Exception("Empty response")

        if (body["ok"] != true) throw Exception(body["error"].toString())
        return body["result"] as Map<String, Any>
    }

    // -------------------------------------------------
    // CHATS LIST
    // -------------------------------------------------
    suspend fun getChats(token: String): List<Chat> {
        if (isDevMode()) return DevModeManager.getMockChats()

        val resp = api.call(
            ApiRequest(
                action = "chats.get",
                token = token
            )
        )

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string()
            Log.e(TAG, "getChats error: ${resp.code()} $errorBody")
            throw Exception("API Error: ${resp.code()} $errorBody")
        }

        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"].toString())

        val list = body["result"] as List<Map<String, Any>>

        return list.map {
            val rawUserId = it["user_id"]
            val userId = when (rawUserId) {
                is Number -> rawUserId.toInt()
                is String -> rawUserId.toIntOrNull()
                else -> null
            }

            Chat(
                id = (it["chat_id"] as Number).toInt(),
                name = it["name"]?.toString(),
                avatar = it["avatar"]?.toString(),
                last_msg = it["last_message"]?.toString(),
                last_time = it["last_time"]?.toString(),
                unread = (it["unread"] as Number).toInt(),
                last_sender_id = (it["last_sender_id"] as Number?)?.toInt(),
                user_id = userId,
                pinned = when (val p = it["pinned"]) {
                    is Boolean -> p
                    is Number -> p.toInt() == 1
                    is String -> p == "1" || p == "true"
                    else -> false
                }
            )
        }
    }
    // -------------------------------------------------
    // GET MESSAGES
    // -------------------------------------------------
    suspend fun getMessages(token: String, chatId: Int): List<Message> {
        if (isDevMode()) return DevModeManager.getMockMessages(chatId)

        val resp = api.call(
            ApiRequest(
                action = "messages.get",
                token = token,
                chat_id = chatId
            )
        )

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string()
            Log.e(TAG, "getMessages error: ${resp.code()} $errorBody")
            throw Exception("API Error: ${resp.code()} $errorBody")
        }

        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"].toString())

        val list = body["result"] as List<Map<String, Any>>

        return list.map {
            val imagesList = it["images"] as? List<String> ?: emptyList()
            Message(
                id = (it["id"] as Number).toInt(),
                chat_id = (it["chat_id"] as Number).toInt(),
                sender_id = (it["sender_id"] as Number).toInt(),
                content = it["content"].toString(),
                sent_at = it["sent_at"].toString(),
                edited_at = it["edited_at"]?.toString(),
                is_read = (it["is_read"] as Number).toInt(),
                avatar = it["avatar"].toString(),
                display_name = it["display_name"].toString(),
                images = imagesList
            )
        }
    }

    suspend fun getMessagesLong(token: String, chatId: Int, lastId: Int?): List<Message> {
        if (isDevMode()) return emptyList()

        val resp = ApiClient.longPollingApi.getMessagesLong(
            token = token,
            chatId = chatId,
            lastId = lastId
        )

        if (!resp.ok) throw Exception(resp.error ?: "Unknown error")

        return resp.result?.map {
            Message(
                id = it.id,
                chat_id = it.chat_id,
                sender_id = it.sender_id,
                content = it.content,
                sent_at = it.sent_at,
                edited_at = it.edited_at,
                is_read = it.is_read,
                avatar = it.avatar,
                display_name = it.display_name,
                images = it.images ?: emptyList()
            )
        } ?: emptyList()
    }

    suspend fun markAsRead(token: String, chatId: Int): Boolean {
        if (isDevMode()) return true
        val resp = ApiClient.api.markAsRead(token = token, chatId = chatId)
        return resp.isSuccessful
    }


    // -------------------------------------------------
    // SEND MESSAGE
    // -------------------------------------------------
    suspend fun sendMessage(token: String, chatId: Int, text: String, images: List<File>? = null): Boolean {
        if (isDevMode()) {
            DevModeManager.addMockMessage(chatId, text, images)
            return true
        }

        val svc = ApiClient.retrofit.create(ApiService::class.java)

        val actionBody = "message.send".toRequestBody("text/plain".toMediaTypeOrNull())
        val tokenBody = token.toRequestBody("text/plain".toMediaTypeOrNull())
        val chatIdBody = chatId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val textBody = text.toRequestBody("text/plain".toMediaTypeOrNull())

        val imageParts = images?.map { file ->
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image[]", file.name, requestFile)
        }

        val resp = svc.sendMessage(
            action = actionBody,
            token = tokenBody,
            chatId = chatIdBody,
            text = textBody,
            image = imageParts
        )

        if (!resp.ok) throw Exception(resp.error ?: "Unknown error")

        return true
    }

    suspend fun editMessage(token: String, messageId: Int, text: String): Boolean {
        if (isDevMode()) return true
        val svc = ApiClient.retrofit.create(ApiService::class.java)
        val resp = svc.editMessage(token = token, messageId = messageId, text = text)
        if (!resp.isSuccessful) throw Exception("API Error: ${resp.code()}")
        val body = resp.body() ?: throw Exception("Empty response")
        if (body["success"] != true) throw Exception(body["error"]?.toString() ?: "Unknown error")
        return true
    }

    suspend fun deleteMessage(token: String, messageId: Int): Boolean {
        if (isDevMode()) return true
        val svc = ApiClient.retrofit.create(ApiService::class.java)
        val resp = svc.deleteMessage(token = token, messageId = messageId)
        if (!resp.isSuccessful) throw Exception("API Error: ${resp.code()}")
        val body = resp.body() ?: throw Exception("Empty response")
        if (body["success"] != true) throw Exception(body["error"]?.toString() ?: "Unknown error")
        return true
    }

    suspend fun searchUsers(token: String, query: String): List<SearchUserItem> {
        if (isDevMode()) return DevModeManager.getMockSearchUsers(query)

        val resp = api.call(
            ApiRequest(
                action = "user.search",
                token = token,
                query = query
            )
        )

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string()
            Log.e(TAG, "searchUsers error: ${resp.code()} $errorBody")
            throw Exception("API Error: ${resp.code()} $errorBody")
        }

        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"].toString())

        val list = body["result"] as List<Map<String, Any>>

        return list.map {
            SearchUserItem(
                id = (it["id"] as Number).toInt(),
                username = it["username"].toString(),
                display_name = it["display_name"]?.toString() ?: "",
                avatar = it["avatar"]?.toString()
            )
        }
    }



    // -------------------------------------------------
    // CREATE CHAT
    // -------------------------------------------------
    suspend fun createChat(token: String, userId: Int): Int {
        if (isDevMode()) return 1

        val resp = api.call(
            ApiRequest(
                action = "chat.create",
                token = token,
                user_id = userId
            )
        )

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string()
            Log.e(TAG, "createChat error: ${resp.code()} $errorBody")
            throw Exception("API Error: ${resp.code()} $errorBody")
        }

        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"].toString())

        val result = body["result"] as Map<String, Any>

        return (result["chat_id"] as Number).toInt()
    }

    suspend fun deleteChat(token: String, chatId: Int, mode: String): Boolean {
        if (isDevMode()) return true
        val svc = ApiClient.retrofit.create(ApiService::class.java)
        val resp = svc.deleteChat(token = token, chatId = chatId, mode = mode)
        if (!resp.isSuccessful) throw Exception("API Error: ${resp.code()}")
        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"]?.toString() ?: "Unknown error")
        return true
    }

    suspend fun pinChat(token: String, chatId: Int): Boolean {
        if (isDevMode()) return true
        val svc = ApiClient.retrofit.create(ApiService::class.java)
        val resp = svc.pinChat(token = token, chatId = chatId)
        if (!resp.isSuccessful) throw Exception("API Error: ${resp.code()}")
        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"]?.toString() ?: "Unknown error")
        return true
    }

    suspend fun unpinChat(token: String, chatId: Int): Boolean {
        if (isDevMode()) return true
        val svc = ApiClient.retrofit.create(ApiService::class.java)
        val resp = svc.unpinChat(token = token, chatId = chatId)
        if (!resp.isSuccessful) throw Exception("API Error: ${resp.code()}")
        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"]?.toString() ?: "Unknown error")
        return true
    }

    // -------------------------------------------------
    // PROFILE EDIT
    // -------------------------------------------------
    suspend fun profileEdit(
        token: String,
        displayName: String?,
        status: String?
    ): Map<String, Any> {
        if (isDevMode()) return mapOf("ok" to true)

        val resp = api.call(
            ApiRequest(
                action = "profile.edit",
                token = token,
                display_name = displayName,
                status = status
            )
        )

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string()
            Log.e(TAG, "profileEdit error: ${resp.code()} $errorBody")
            throw Exception("API Error: ${resp.code()} $errorBody")
        }

        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"].toString())

        return body
    }

    // -------------------------------------------------
    // PROFILE EDIT — MULTIPART
    // -------------------------------------------------
    suspend fun profileEditMultipart(
        token: String,
        displayName: String?,
        status: String?,
        avatar: MultipartBody.Part?,
        cover: MultipartBody.Part? = null
    ): Map<String, Any> {
        if (isDevMode()) return mapOf("ok" to true)

        val svc = ApiClient.retrofit.create(ApiService::class.java)

        val actionBody = "profile.edit".toRequestBody("text/plain".toMediaTypeOrNull())
        val tokenBody = token.toRequestBody("text/plain".toMediaTypeOrNull())
        val nameBody = displayName?.toRequestBody("text/plain".toMediaTypeOrNull())
        val statusBody = status?.toRequestBody("text/plain".toMediaTypeOrNull())

        val resp = svc.profileEditMultipart(
            actionBody,
            tokenBody,
            nameBody,
            statusBody,
            avatar,
            cover
        )

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string()
            throw Exception("API Error: ${resp.code()} $errorBody")
        }

        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"].toString())

        return body
    }

    // -------------------------------------------------
    // GET USER PROFILE
    // -------------------------------------------------
    suspend fun getUserProfile(token: String, userId: Int): UserProfile {
        if (isDevMode()) return DevModeManager.getMockUserProfile()

        val resp = api.call(
            ApiRequest(
                action = "get.profile",
                token = token,
                user_id = userId
            )
        )

        if (!resp.isSuccessful) throw Exception("API Error: ${resp.code()}")
        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"].toString())

        val r = body["result"] as Map<String, Any>
        
        // Обработка нового формата премиум-данных
        val premiumData = r["premium"] as? Map<String, Any>
        val isActive = when(val active = premiumData?.get("active")) {
            is Boolean -> active
            is Number -> active.toInt() == 1
            is String -> active == "1" || active == "true"
            else -> false
        }

        return UserProfile(
            id = (r["id"] as Number).toInt(),
            username = r["username"]?.toString() ?: r["login"]?.toString() ?: "",
            display_name = r["display_name"]?.toString(),
            avatar = r["avatar"]?.toString(),
            status = r["status"]?.toString(),
            friends_count = (r["friends_count"] as? Number)?.toInt() ?: 0,
            followers_count = (r["followers_count"] as? Number)?.toInt() ?: 0,
            is_premium = isActive,
            premium_info = premiumData?.let {
                PremiumInfo(
                    active = isActive,
                    plan = it["plan"]?.toString(),
                    expires_at = it["expires_at"]?.toString()
                )
            },
            cover = r["cover"]?.toString()
        )
    }

    // -------------------------------------------------
    // SUBSCRIPTIONS
    // -------------------------------------------------
    suspend fun checkSubscription(token: String): SubscriptionCheckResult? {
        if (isDevMode()) return SubscriptionCheckResult(active = 1, plan = "lifetime", expires_at = null)

        val svc = ApiClient.retrofit.create(ApiService::class.java)
        val resp = svc.checkSubscription(token = token)

        if (!resp.ok) throw Exception(resp.error ?: "Unknown error")
        return resp.result
    }

    suspend fun buySubscription(token: String, plan: String): SubscriptionBuyResult? {
        if (isDevMode()) return SubscriptionBuyResult(message = "Success", plan = plan, expires_at = "2025-12-31")

        val svc = ApiClient.retrofit.create(ApiService::class.java)
        val resp = svc.buySubscription(token = token, plan = plan)

        if (!resp.ok) throw Exception(resp.error ?: "Unknown error")
        return resp.result
    }
}
