package com.kia.blagogramapp.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

// --- OpenAI Style ---
data class AiRequest(
    val model: String,
    val messages: List<AiChatMessage>
)

data class AiChatMessage(
    val role: String,
    val content: String
)

data class AiResponse(
    val choices: List<AiChoice>
)

data class AiChoice(
    val message: AiChatMessage
)

// --- Gemini Native Style (generateContent) ---
data class GeminiRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("system_instruction") val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    @SerializedName("role") val role: String? = null, // "user" или "model"
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String
)

data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent?
)

interface AiApi {
    @POST
    suspend fun getCompletion(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Body request: AiRequest
    ): AiResponse

    @POST
    suspend fun getGeminiCompletion(
        @Url url: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
