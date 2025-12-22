package com.kia.blagogramapp.api

import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApi {
    @GET("tracks")
    suspend fun searchTracks(
        @Query("client_id") clientId: String = "8013f7cf",
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("search") query: String,
        @Query("include") include: String = "musicinfo"
    ): JamendoResponse

    @GET("tracks")
    suspend fun getPopularTracks(
        @Query("client_id") clientId: String = "8013f7cf",
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("order") order: String = "popularity_total"
    ): JamendoResponse
}
