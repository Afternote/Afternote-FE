package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternotePlaylist
import com.afternote.feature.afternote.data.dto.AfternoteSong
import com.afternote.feature.afternote.domain.model.author.PlaylistSongPayload
import com.afternote.feature.afternote.domain.model.author.PlaylistWritePayload

fun PlaylistWritePayload.toDto() =
    AfternotePlaylist(
        profilePhoto = profilePhoto,
        atmosphere = atmosphere,
        memorialPhotoUrl = memorialPhotoUrl,
        songs = songs.toDto(),
        memorialVideo = memorialVideo?.toDto(),
    )

fun PlaylistSongPayload.toDto() =
    AfternoteSong(
        id = id,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )

fun List<PlaylistSongPayload>.toDto() =
    map {
        it.toDto()
    }
