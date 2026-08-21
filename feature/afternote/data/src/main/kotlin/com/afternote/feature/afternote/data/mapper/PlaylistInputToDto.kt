package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternotePlaylistDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload

fun MemorialWritePayload.toDto() =
    AfternotePlaylistDto(
        memorialPhotoUrl = memorialPhotoUrl,
        songs = songs.toDto(),
        memorialVideo = memorialVideo?.toDto(),
    )

fun MemorialSongPayload.toDto() =
    AfternoteSongDto(
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )

fun List<MemorialSongPayload>.toDto() =
    map {
        it.toDto()
    }
