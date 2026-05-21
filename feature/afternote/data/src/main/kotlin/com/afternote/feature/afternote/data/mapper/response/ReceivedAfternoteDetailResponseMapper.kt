package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDetailResponse
import com.afternote.feature.afternote.data.dto.ReceivedCredentialsInfo
import com.afternote.feature.afternote.data.dto.ReceivedPlaylistInfo
import com.afternote.feature.afternote.data.dto.ReceivedSongInfo
import com.afternote.feature.afternote.data.mapper.categoryToServiceType
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAccountCredentials
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedPlaylistDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedPlaylistSong

fun ReceivedAfternoteDetailResponse.toDomain(): ReceivedAfternoteDetail =
    ReceivedAfternoteDetail(
        title = title,
        senderName = senderName,
        createdAt = createdAt?.let(::formatDateFromServer),
        category = category,
        type = category?.let(::categoryToServiceType),
        actions = actions,
        leaveMessage = leaveMessage,
        playlist = playlist?.toDomain(),
        credentials = credentials?.toDomain(),
    )

private fun ReceivedCredentialsInfo.toDomain(): ReceivedAccountCredentials =
    ReceivedAccountCredentials(
        id = id,
        password = password,
    )

private fun ReceivedPlaylistInfo.toDomain(): ReceivedPlaylistDetail =
    ReceivedPlaylistDetail(
        songs = songs.map { it.toDomain() },
        atmosphere = atmosphere,
        memorialVideoUrl = memorialVideo?.videoUrl,
        memorialThumbnailUrl = memorialVideo?.thumbnailUrl,
    )

private fun ReceivedSongInfo.toDomain(): ReceivedPlaylistSong =
    ReceivedPlaylistSong(
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )
