package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDetailDto
import com.afternote.feature.afternote.data.dto.ReceivedCredentialsDto
import com.afternote.feature.afternote.data.dto.ReceivedPlaylistDto
import com.afternote.feature.afternote.data.dto.ReceivedSongDto
import com.afternote.feature.afternote.data.mapper.categoryToAfternoteType
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAccountCredentials
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedPlaylistDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedPlaylistSong

fun ReceivedAfternoteDetailDto.toDomain(): ReceivedAfternoteDetail =
    ReceivedAfternoteDetail(
        title = title,
        senderName = senderName,
        createdAt = createdAt?.let(::formatDateFromServer),
        category = category,
        type = category?.let(::categoryToAfternoteType),
        processingMethods = processingMethods,
        leaveMessage = leaveMessage,
        playlist = playlist?.toDomain(),
        credentials = credentials?.toDomain(),
    )

private fun ReceivedCredentialsDto.toDomain(): ReceivedAccountCredentials =
    ReceivedAccountCredentials(
        id = id,
        password = password,
    )

private fun ReceivedPlaylistDto.toDomain(): ReceivedPlaylistDetail =
    ReceivedPlaylistDetail(
        songs = songs.map { it.toDomain() },
        atmosphere = atmosphere,
        memorialVideoUrl = memorialVideo?.videoUrl,
        memorialThumbnailUrl = memorialVideo?.thumbnailUrl,
    )

private fun ReceivedSongDto.toDomain(): ReceivedPlaylistSong =
    ReceivedPlaylistSong(
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )
