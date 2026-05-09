package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.AddSongCallbacks
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.AddSongEntry
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.AddSongViewModel

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
