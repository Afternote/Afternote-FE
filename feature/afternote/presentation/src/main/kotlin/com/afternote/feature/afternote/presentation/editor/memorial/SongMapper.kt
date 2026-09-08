package com.afternote.feature.afternote.presentation.editor.memorial
import com.afternote.feature.afternote.domain.model.author.playlist.SearchedSong
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

fun SearchedSong.toDisplay() =
    PlaylistSongDisplay(
        selectionKey = selectionKey,
        title = title,
        artist = artist,
        albumImageUrl = albumImageUrl,
    )
