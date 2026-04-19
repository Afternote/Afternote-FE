package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongCallbacks
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongEntry
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongViewModel
import com.afternote.feature.afternote.presentation.author.editor.state.MemorialPlaylistStateHolder

@Composable
internal fun AfternoteAddSongNavigation(
    onPopBackStack: () -> Unit,
    playlistStateHolder: MemorialPlaylistStateHolder,
    viewModel: AddSongViewModel,
) {
    AddSongEntry(
        viewModel = viewModel,
        callbacks =
            AddSongCallbacks(
                onBackClick = onPopBackStack,
                onSongsAdded = { added ->
                    added.forEach { playlistStateHolder.addSong(it) }
                    onPopBackStack()
                },
            ),
    )
}
