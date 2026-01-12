package com.kia.blagogramapp.model

data class UserProfile(
    val id: Int,
    val username: String,
    val display_name: String?,
    val avatar: String?,
    val status: String?,
    val friends_count: Int = 0,
    val followers_count: Int = 0,
    val is_premium: Boolean = false,
    val premium_info: PremiumInfo? = null
)

data class PremiumInfo(
    val active: Boolean,
    val plan: String?,
    val expires_at: String?
)
