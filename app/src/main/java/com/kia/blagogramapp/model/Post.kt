package com.kia.blagogramapp.model

import com.google.gson.annotations.SerializedName

data class Post(
    val id: Int,
    val title: String?,
    val content: String,
    val image: String?,
    @SerializedName("is_dev")
    val isDev: Int,
    @SerializedName("created_at")
    val createdAt: String,
    val likes: Int,
    val liked: Boolean
)
