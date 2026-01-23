package com.kia.blagogramapp.database

import androidx.room.*

@Dao
interface ChatMediaDao {
    @Query("SELECT * FROM chat_media_cache WHERE chatId = :chatId")
    suspend fun getChatMedia(chatId: Int): ChatMediaCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMedia(cache: ChatMediaCacheEntity)

    @Query("DELETE FROM chat_media_cache WHERE chatId = :chatId")
    suspend fun deleteChatMedia(chatId: Int)
}
