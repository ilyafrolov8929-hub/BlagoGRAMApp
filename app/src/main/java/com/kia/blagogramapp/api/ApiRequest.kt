package com.kia.blagogramapp.api

data class ApiRequest(
    val action: String,
    val token: String? = null,

    val login: String? = null,
    val password: String? = null,

    val chat_id: Int? = null,
    val limit: Int? = null,
    val before_id: Int? = null,

    val text: String? = null,
    val message: String? = null,

    val user_id: Int? = null,

    val display_name: String? = null,
    val status: String? = null,
    val theme: String? = null,
    val avatar: String? = null,
    val avatar_data: String? = null,
    val offset: Int? = null,
    val query: String? = null
)
