package com.kia.blagogramapp.api

import com.google.gson.annotations.SerializedName

data class JamendoResponse(
    @SerializedName("results") val results: List<JamendoTrack>
)

data class JamendoTrack(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("artist_name") val artistName: String,
    @SerializedName("duration") val duration: Int,
    @SerializedName("image") val image: String,
    @SerializedName("audio") val audio: String,
    @SerializedName("album_name") val albumName: String
)
