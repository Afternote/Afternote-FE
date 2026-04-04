package com.afternote.feature.afternote.presentation.author.editor.playlist
import com.afternote.feature.afternote.domain.model.playlist.SearchedSong
import com.afternote.feature.afternote.presentation.shared.model.PlaylistSongDisplay

fun SearchedSong.toDisplay() =
    PlaylistSongDisplay(
        id = id,
        title = title,
        artist = artist,
    )
