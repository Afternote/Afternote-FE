package com.afternote.feature.afternote.presentation.author.editor.playlist

import androidx.compose.runtime.Immutable

/**
 * 노래 데이터 모델
 */
@Immutable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val albumCoverUrl: String? = null,
)
