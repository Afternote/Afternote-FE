package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.AddSongEntry
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.AddSongViewModel
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song

@Composable
internal fun AfternoteAddSongNavigation(
    onPopBackStack: () -> Unit,
    onSongsAdded: (List<Song>) -> Unit,
    viewModel: AddSongViewModel,
) {
    AddSongEntry(
        viewModel = viewModel,
        onBackClick = onPopBackStack,
        onSongsAdded = { added ->
            onSongsAdded(added)
            onPopBackStack()
        },
    )
}
