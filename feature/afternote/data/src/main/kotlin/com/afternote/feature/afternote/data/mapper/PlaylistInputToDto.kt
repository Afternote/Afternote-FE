package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternotePlaylistDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.domain.model.author.PlaylistSongPayload
import com.afternote.feature.afternote.domain.model.author.PlaylistWritePayload

fun PlaylistWritePayload.toDto() =
    AfternotePlaylistDto(
        profilePhoto = profilePhoto,
        atmosphere = atmosphere,
        memorialPhotoUrl = memorialPhotoUrl,
        songs = songs.toDto(),
        memorialVideo = memorialVideo?.toDto(),
    )

fun PlaylistSongPayload.toDto() =
    AfternoteSongDto(
        id = id,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )

fun List<PlaylistSongPayload>.toDto() =
    map {
        it.toDto()
    }
