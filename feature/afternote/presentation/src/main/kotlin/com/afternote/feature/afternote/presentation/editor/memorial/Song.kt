package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 에디터 플레이리스트의 노래.
 *
 * [selectionKey]는 선택·삭제 등 UI 내부 식별에만 사용하며 서버 요청에는 포함하지 않는다.
 */
@Immutable
@Serializable
data class Song(
    val selectionKey: String,
    val title: String,
    val artist: String,
    val albumCoverUrl: String? = null,
)
