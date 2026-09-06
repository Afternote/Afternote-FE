package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.annotation.StringRes
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

sealed interface ReceiverMemorialPlaylistUiState {
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
