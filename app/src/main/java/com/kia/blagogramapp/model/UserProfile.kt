package com.kia.blagogramapp.model

data class UserProfile(
    val id: Int,
    val username: String,
    val display_name: String?,
    val avatar: String?,
    val status: String?,
    val friends_count: Int = 0,
    val followers_count: Int = 0
)
