package com.kia.blagogramapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Int,
    val title: String?,
    val content: String,
    val image: String?,
    val isDev: Int,
    val createdAt: String,
    val likes: Int,
    val liked: Boolean
)
