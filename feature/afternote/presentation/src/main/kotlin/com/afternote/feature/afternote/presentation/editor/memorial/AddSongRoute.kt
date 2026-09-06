package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.runtime.Composable

@Composable
internal fun AfternoteAddSongNavigation(
    onPopBackStack: () -> Unit,
    onSongsAdded: (List<Song>) -> Unit,
    viewModel: AddSongViewModel,
) {
    AddSongEntry(
        viewModel = viewModel,
        onBackClick = onPopBackStack,
        onSongsAdded = onSongsAdded,
    )
}
