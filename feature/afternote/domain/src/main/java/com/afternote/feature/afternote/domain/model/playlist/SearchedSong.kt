package com.afternote.feature.afternote.domain.model.playlist

data class SearchedSong(
    val id: String,
    val title: String,
    val artist: String,
    val albumImageUrl: String? = null,
)
