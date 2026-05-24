package com.afternote.feature.afternote.presentation.author.editor.memorial.playlist

import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

/**
 * UI state for Add Song screen (search-driven list from API).
 */
data class AddSongUiState(
    val songs: List<PlaylistSongDisplay> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
