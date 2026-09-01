package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicSearchResponseDto(
    @SerialName("tracks") val tracks: List<MusicTrackDto>,
)

@Serializable
data class MusicTrackDto(
    @SerialName("artist") val artist: String,
    @SerialName("title") val title: String,
    @SerialName("albumImageUrl") val albumImageUrl: String? = null,
)
