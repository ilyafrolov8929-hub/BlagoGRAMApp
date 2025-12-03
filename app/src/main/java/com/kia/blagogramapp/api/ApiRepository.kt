package com.kia.blagogramapp.api

import android.util.Log
import com.kia.blagogramapp.BlagoApi
import com.kia.blagogramapp.model.Chat
import com.kia.blagogramapp.model.Message
import com.kia.blagogramapp.models.SearchUserItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ApiRepository(private val api: BlagoApi) {

    private val TAG = "ApiRepository"

    // -------------------------------------------------
    // LOGIN
    // -------------------------------------------------
    suspend fun login(login: String, password: String): Map<String, Any> {
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
            Chat(
                id = (it["chat_id"] as Number).toInt(),
                name = it["name"]?.toString(),
                avatar = it["avatar"]?.toString(),
                last_msg = it["last_message"]?.toString(),
                last_time = it["last_time"]?.toString(),
                unread = (it["unread"] as Number).toInt(),
                last_sender_id = (it["last_sender_id"] as Number?)?.toInt()
            )
        }
    }
    // -------------------------------------------------
    // GET MESSAGES
    // -------------------------------------------------
    suspend fun getMessages(token: String, chatId: Int): List<Message> {

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
                is_read = (it["is_read"] as Number).toInt(),
                avatar = it["avatar"].toString(),
                display_name = it["display_name"].toString(),
                images = imagesList
            )
        }
    }


    // -------------------------------------------------
    // SEND MESSAGE
    // -------------------------------------------------
    suspend fun sendMessage(token: String, chatId: Int, text: String, images: List<File>? = null): Boolean {

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

    suspend fun searchUsers(token: String, query: String): List<SearchUserItem> {

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

    // -------------------------------------------------
    // PROFILE EDIT — JSON (если нет файла)
    // -------------------------------------------------
    suspend fun profileEdit(
        token: String,
        displayName: String?,
        status: String?
    ): Map<String, Any> {

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
    // PROFILE EDIT — MULTIPART (если есть файл)
    // -------------------------------------------------
    suspend fun profileEditMultipart(
        token: String,
        displayName: String?,
        status: String?,
        avatar: MultipartBody.Part?
    ): Map<String, Any> {

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
            avatar
        )

        Log.d(TAG, "multipart code=${resp.code()} ok=${resp.isSuccessful}")

        if (!resp.isSuccessful) {
            val errorBody = resp.errorBody()?.string()
            throw Exception("API Error: ${resp.code()} $errorBody")
        }

        val body = resp.body() ?: throw Exception("Empty response")
        if (body["ok"] != true) throw Exception(body["error"].toString())

        return body
    }
}
