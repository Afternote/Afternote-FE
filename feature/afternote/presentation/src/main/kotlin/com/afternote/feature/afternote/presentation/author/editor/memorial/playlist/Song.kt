package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 노래 데이터 모델
 */
@Immutable
@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumCoverUrl: String? = null,
)
