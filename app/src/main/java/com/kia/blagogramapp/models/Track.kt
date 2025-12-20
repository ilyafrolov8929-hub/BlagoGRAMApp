package com.kia.blagogramapp.models

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String, // Path to file
    val duration: Long,
    val albumArtUri: String? = null
)
