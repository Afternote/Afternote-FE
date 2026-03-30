package com.afternote.feature.afternote.presentation.shared.model.dummy

import com.afternote.feature.afternote.presentation.shared.component.list.AlbumCover

/**
 * Default album cover list for playlists (Previews and dummy state).
 * Replace with real data when loaded from API.
 */
object AlbumDummies {
    val list: List<AlbumCover> =
        listOf(
            AlbumCover("1"),
            AlbumCover("2"),
            AlbumCover("3"),
            AlbumCover("4"),
        )
}
