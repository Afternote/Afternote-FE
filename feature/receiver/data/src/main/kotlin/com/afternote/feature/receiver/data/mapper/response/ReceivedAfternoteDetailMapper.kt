package com.afternote.feature.receiver.data.mapper.response

import com.afternote.feature.afternote.data.mapper.afternoteTypeFromServerCategory
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.afternote.data.mapper.toLeaveMessageBlocks
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDetailDto
import com.afternote.feature.receiver.data.dto.ReceivedCredentialsDto
import com.afternote.feature.receiver.data.dto.ReceivedPlaylistDto
import com.afternote.feature.receiver.data.dto.ReceivedSongDto
import com.afternote.feature.receiver.domain.model.ReceivedAccountCredentials
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail
import com.afternote.feature.receiver.domain.model.ReceivedPlaylistDetail
import com.afternote.feature.receiver.domain.model.ReceivedPlaylistSong

fun ReceivedAfternoteDetailDto.toDomain(): ReceivedAfternoteDetail =
    ReceivedAfternoteDetail(
        serviceName = serviceName,
        senderName = senderName,
        createdAt = createdAt?.let(::formatDateFromServer),
        type =
            requireNotNull(category?.let(::afternoteTypeFromServerCategory)) {
                "해석할 수 없는 애프터노트 종류다: id=$id category=$category"
            },
        processingMethods = processingMethods.orEmpty(),
        leaveMessageBlocks = leaveMessage.toLeaveMessageBlocks(),
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
