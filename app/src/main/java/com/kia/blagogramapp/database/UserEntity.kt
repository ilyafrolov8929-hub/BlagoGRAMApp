package com.kia.blagogramapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val displayName: String?,
    val avatar: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)
