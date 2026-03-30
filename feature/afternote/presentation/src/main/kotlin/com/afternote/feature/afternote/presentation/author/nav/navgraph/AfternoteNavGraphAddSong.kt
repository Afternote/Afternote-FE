package com.afternote.feature.afternote.presentation.author.nav.navgraph

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.afternote.feature.afternote.presentation.author.edit.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.edit.ui.addsong.AddSongCallbacks
import com.afternote.feature.afternote.presentation.author.edit.ui.addsong.AddSongScreen
import com.afternote.feature.afternote.presentation.author.edit.ui.addsong.AddSongViewModel

@Composable
internal fun AfternoteAddSongRouteContent(
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
