package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.annotation.StringRes
import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

internal sealed interface ReceiverMemorialPlaylistUiState : UiState {
    data object Loading : ReceiverMemorialPlaylistUiState

    data class Success(
        val senderName: String,
        val songs: List<PlaylistSongDisplay>,
        val memorialVideoUrl: String?,
        val memorialThumbnailUrl: String?,
    ) : ReceiverMemorialPlaylistUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : ReceiverMemorialPlaylistUiState
}
