package com.afternote.feature.afternote.presentation.receiver.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.loading.LoadingBody
import com.afternote.feature.afternote.presentation.shared.detail.DetailLoadErrorContent

@Composable
internal fun ReceiverMemorialPlaylistRoute(
    viewModel: ReceiverMemorialPlaylistViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(ReceiverMemorialPlaylistIntent.RefreshOnReturn)
    }
    ReceiverMemorialPlaylistContent(state, viewModel::onIntent, onBackClick)
}

@Composable
private fun ReceiverMemorialPlaylistContent(
    state: ReceiverMemorialPlaylistUiState,
    onIntent: (ReceiverMemorialPlaylistIntent) -> Unit,
    onBackClick: () -> Unit,
) {
    when (state) {
        ReceiverMemorialPlaylistUiState.Loading -> {
            LoadingBody()
        }

        is ReceiverMemorialPlaylistUiState.Error -> {
            DetailLoadErrorContent(
                messageRes = state.messageRes,
                onBackClick = onBackClick,
                onRetryClick = { onIntent(ReceiverMemorialPlaylistIntent.Retry) },
            )
        }

        is ReceiverMemorialPlaylistUiState.Success -> {
            MemorialPlaylistScreen(senderName = state.senderName, songs = state.songs, onBackClick = onBackClick)
        }
    }
}
