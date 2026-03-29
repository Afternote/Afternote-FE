package com.afternote.feature.afternote.presentation.mapper

import com.afternote.feature.afternote.domain.model.playlist.SearchedSong
import com.afternote.feature.afternote.presentation.edit.addsong.PlaylistSongDisplay

fun SearchedSong.toDisplay() =
    PlaylistSongDisplay(
        id = id,
        title = title,
        artist = artist,
    )
