package com.afternote.feature.afternote.presentation.editor.memorial

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

internal sealed interface AddSongIntent : MviIntent {
    data class Search(
        val query: String,
    ) : AddSongIntent

    data object ConsumeError : AddSongIntent
}

internal sealed interface AddSongReducerEvent : ReducerEvent {
    data class QueryChanged(
        val query: String,
    ) : AddSongReducerEvent

    data object SearchStarted : AddSongReducerEvent

    data class SongsLoaded(
        val songs: List<PlaylistSongDisplay>,
    ) : AddSongReducerEvent

    data object SearchFailed : AddSongReducerEvent

    data object ErrorConsumed : AddSongReducerEvent
}
