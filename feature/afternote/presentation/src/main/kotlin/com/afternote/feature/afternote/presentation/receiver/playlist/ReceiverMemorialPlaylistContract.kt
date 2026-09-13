package com.afternote.feature.afternote.presentation.receiver.playlist

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent

internal sealed interface ReceiverMemorialPlaylistIntent : MviIntent {
    data object Retry : ReceiverMemorialPlaylistIntent

    data object RefreshOnReturn : ReceiverMemorialPlaylistIntent
}

internal sealed interface ReceiverMemorialPlaylistReducerEvent : ReducerEvent {
    data object Loading : ReceiverMemorialPlaylistReducerEvent

    data class ContentLoaded(
        val content: ReceiverMemorialPlaylistUiState.Success,
    ) : ReceiverMemorialPlaylistReducerEvent

    data class LoadFailed(
        val keepsContent: Boolean,
    ) : ReceiverMemorialPlaylistReducerEvent
}
