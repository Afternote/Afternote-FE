package com.afternote.feature.afternote.presentation.author.editor.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 노래 추가 Entry.
 *
 * ViewModel에서 데이터를 로드·가공하고, Screen에 전달만 합니다.
 */
@Composable
fun AddSongEntry(
    viewModel: AddSongViewModel,
    callbacks: AddSongCallbacks,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AddSongScreen(
        uiState = uiState,
        onSearchQueryChange = { viewModel.onEvent(AddSongEvent.SearchQueryChange(it)) },
        callbacks = callbacks,
        modifier = modifier,
    )
}
