package com.kia.blagogramapp.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {
    @Query("SELECT * FROM ai_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AiMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: AiMessage): Long

    @Query("DELETE FROM ai_messages")
    suspend fun clearAll()
}
