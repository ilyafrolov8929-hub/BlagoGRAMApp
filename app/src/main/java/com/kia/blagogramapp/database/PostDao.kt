package com.kia.blagogramapp.database

import androidx.room.*

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY created_at DESC")
    suspend fun getAll(): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

    @Update
    suspend fun update(post: PostEntity)
}
