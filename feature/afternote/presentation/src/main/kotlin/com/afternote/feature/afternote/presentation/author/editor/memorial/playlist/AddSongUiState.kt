package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import com.afternote.core.ui.UiText
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * UI state for Add Song screen (search-driven list from API).
 *
 * errorMessage 는 [UiText] 로 들고 있어 ViewModel 이 Context/Resources 에 접근하지 않는다.
 * Composable 측에서 [com.afternote.core.ui.asString] 으로 변환.
 */
data class AddSongUiState(
    val songs: List<PlaylistSongDisplay> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
)
