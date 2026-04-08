package com.afternote.feature.afternote.presentation.author.editor.playlist
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay
import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for Add Song screen ViewModel (production uses [AddSongViewModel], Preview uses Fake).
 */
interface AddSongViewModelContract {
    val uiState: StateFlow<AddSongUiState>

    fun onEvent(event: AddSongEvent)
}

/**
 * UI events for Add Song screen.
 */
sealed interface AddSongEvent {
    data class SearchQueryChange(
        val query: String,
    ) : AddSongEvent
}

/**
 * UI state for Add Song screen (search-driven list from API).
 */
data class AddSongUiState(
    val songs: List<PlaylistSongDisplay> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
