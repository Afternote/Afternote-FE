package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternotePlaylist
import com.afternote.feature.afternote.data.dto.AfternoteSong
import com.afternote.feature.afternote.domain.model.input.PlaylistInput
import com.afternote.feature.afternote.domain.model.input.SongInput

fun PlaylistInput.toDto() =
    AfternotePlaylist(
        profilePhoto = profilePhoto,
        atmosphere = atmosphere,
        memorialPhotoUrl = memorialPhotoUrl,
        songs = songs.toDto(),
        memorialVideo = memorialVideo?.toDto(),
    )

fun SongInput.toDto() =
    AfternoteSong(
        id = id,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )

fun List<SongInput>.toDto() =
    map {
        it.toDto()
    }
