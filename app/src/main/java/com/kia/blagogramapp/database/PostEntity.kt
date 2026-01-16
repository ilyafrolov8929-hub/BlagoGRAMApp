package com.kia.blagogramapp.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "author_id") val authorId: Int,
    val title: String?,
    val content: String,
    val image: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    val likes: Int,
    val liked: Boolean
)
