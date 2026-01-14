package com.kia.blagogramapp.database

import androidx.room.*

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY pinned DESC, lastTime DESC")
    suspend fun getAll(): List<ChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chats: List<ChatEntity>)

    @Query("DELETE FROM chats")
    suspend fun deleteAll()
}
