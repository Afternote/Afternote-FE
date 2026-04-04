package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongCallbacks
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongScreen
import com.afternote.feature.afternote.presentation.author.editor.playlist.AddSongViewModel

@Composable
internal fun AfternoteAddSongDestination(
    navController: NavController,
    playlistStateHolder: MemorialPlaylistStateHolder,
    viewModel: AddSongViewModel,
) {
    AddSongScreen(
        viewModel = viewModel,
        callbacks =
            AddSongCallbacks(
                onBackClick = { navController.popBackStack() },
                onSongsAdded = { added ->
                    added.forEach { playlistStateHolder.addSong(it) }
                    navController.popBackStack()
                },
            ),
    )
}
