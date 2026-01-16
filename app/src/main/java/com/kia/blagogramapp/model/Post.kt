package com.kia.blagogramapp.model

import com.google.gson.annotations.SerializedName

data class Post(
    val id: Int = 0,
    @SerializedName(value = "author_id", alternate = ["user_id", "userId", "authorId"])
    val authorId: Int = 0,
    val title: String? = null,
    val content: String = "",
    val image: String? = null,
    @SerializedName("is_dev")
    val isDev: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String = "",
    @SerializedName(value = "likes", alternate = ["likes_count"])
    val likes: Int = 0,
    @SerializedName(value = "liked", alternate = ["liked_by_me"])
    val liked: Boolean = false
)
