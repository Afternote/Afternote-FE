package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.edit.playlist.AddSongCallbacks
import com.afternote.feature.afternote.presentation.author.edit.playlist.AddSongScreen
import com.afternote.feature.afternote.presentation.author.edit.playlist.AddSongViewModel

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
