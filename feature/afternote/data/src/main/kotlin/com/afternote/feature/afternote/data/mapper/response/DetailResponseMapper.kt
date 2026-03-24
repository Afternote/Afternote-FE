package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.AfternoteCredentials
import com.afternote.feature.afternote.data.dto.AfternoteDetailReceiver
import com.afternote.feature.afternote.data.dto.AfternotePlaylist
import com.afternote.feature.afternote.data.dto.AfternoteSong
import com.afternote.feature.afternote.data.dto.response.AfternoteDetailResponse
import com.afternote.feature.afternote.data.mapper.categoryToServiceType
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.domain.model.DetailCredentials
import com.afternote.feature.afternote.domain.model.DetailProcessing
import com.afternote.feature.afternote.domain.model.DetailReceiver
import com.afternote.feature.afternote.domain.model.DetailTimestamps
import com.afternote.feature.afternote.domain.model.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.playlist.PlaylistDetail
import com.afternote.feature.afternote.domain.model.playlist.PlaylistDetailMemorialMedia

fun AfternoteDetailResponse.toDetailDomain(): Detail =
    Detail(
        id = afternoteId,
        category = category,
        title = title,
        timestamps = toTimestamps(),
        type = categoryToServiceType(category),
        credentials = credentials?.toDomain(),
        receivers = receivers.toDomain(),
        processing = toProcessing(),
        playlist = playlist?.toDomain(),
    )

fun List<AfternoteDetailReceiver>?.toDomain() =
    this?.map { a ->
        a.toDomain()
    } ?: emptyList()

fun AfternoteDetailResponse.toTimestamps(): DetailTimestamps =
    DetailTimestamps(
        createdAt = formatDateFromServer(createdAt),
        updatedAt = formatDateFromServer(updatedAt),
    )

fun AfternoteDetailResponse.toProcessing() =
    DetailProcessing(
        method = processMethod,
        actions = actions ?: emptyList(),
        leaveMessage = leaveMessage,
    )

fun AfternoteCredentials.toDomain() =
    DetailCredentials(
        id = id,
        password = password,
    )

fun AfternotePlaylist.toDomain() =
    PlaylistDetail(
        profilePhoto = profilePhoto,
        atmosphere = atmosphere,
        songs = songs.map { it.toDomain() },
        playlistDetailMemorialMedia = toMemorialMedia(),
    )

fun AfternotePlaylist.toMemorialMedia() =
    PlaylistDetailMemorialMedia(
        photoUrl = memorialPhotoUrl ?: profilePhoto,
        videoUrl = memorialVideo?.videoUrl,
        thumbnailUrl = memorialVideo?.thumbnailUrl,
    )

fun AfternoteDetailReceiver.toDomain() =
    DetailReceiver(
        receiverId = receiverId,
        name = name ?: "",
        relation = relation ?: "",
        phone = phone ?: "",
    )

fun AfternoteSong.toDomain() =
    DetailSong(
        id = id,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )
