package com.afternote.feature.afternote.presentation.author.editor.playlist

/**
 * UI events for Add Song screen.
 */
sealed interface AddSongEvent {
    data class SearchQueryChange(
        val query: String,
    ) : AddSongEvent
}
