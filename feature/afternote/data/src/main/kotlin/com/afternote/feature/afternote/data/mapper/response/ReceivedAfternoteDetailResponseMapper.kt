package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.ReceivedAfternoteDetailResponse
import com.afternote.feature.afternote.data.dto.ReceivedPlaylistInfo
import com.afternote.feature.afternote.data.dto.ReceivedSongInfo
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedPlaylistDetail
import com.afternote.feature.afternote.domain.model.receiver.ReceivedPlaylistSong

fun ReceivedAfternoteDetailResponse.toDomain(): ReceivedAfternoteDetail =
    ReceivedAfternoteDetail(
        title = title,
        senderName = senderName,
        createdAt = createdAt,
        category = category,
        actions = actions,
        leaveMessage = leaveMessage,
        playlist = playlist?.toDomain(),
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
