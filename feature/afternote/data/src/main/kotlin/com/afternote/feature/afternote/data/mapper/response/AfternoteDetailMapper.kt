package com.afternote.feature.afternote.data.mapper.response

import com.afternote.feature.afternote.data.dto.AfternoteCredentialsDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailDto
import com.afternote.feature.afternote.data.dto.AfternoteDetailReceiverDto
import com.afternote.feature.afternote.data.dto.AfternotePlaylistDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.data.mapper.categoryToAfternoteType
import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DetailProcessing
import com.afternote.feature.afternote.domain.model.author.DetailReceiver
import com.afternote.feature.afternote.domain.model.author.DetailTimestamps
import com.afternote.feature.afternote.domain.model.author.playlist.DetailSong
import com.afternote.feature.afternote.domain.model.author.playlist.PlaylistDetail
import com.afternote.feature.afternote.domain.model.author.playlist.PlaylistDetailMemorialMedia

fun AfternoteDetailDto.toDetailDomain(): Detail =
    Detail(
        id = afternoteId,
        category = category,
        title = title,
        timestamps = toTimestamps(),
        type = categoryToAfternoteType(category),
        credentials = credentials?.toDomain(),
        receivers = receivers.toDomain(),
        processing = toProcessing(),
        playlist = playlist?.toDomain(),
    )

private fun List<AfternoteDetailReceiverDto>?.toDomain() =
    this?.map { a ->
        a.toDomain()
    } ?: emptyList()

private fun AfternoteDetailDto.toTimestamps(): DetailTimestamps =
    DetailTimestamps(
        createdAt = formatDateFromServer(createdAt),
        updatedAt = formatDateFromServer(updatedAt),
    )

private fun AfternoteDetailDto.toProcessing() =
    DetailProcessing(
        actions = actions ?: emptyList(),
        leaveMessage = leaveMessage,
    )

private fun AfternoteCredentialsDto.toDomain() =
    DetailCredentials(
        id = id,
        password = password,
    )

private fun AfternotePlaylistDto.toDomain() =
    PlaylistDetail(
        profilePhoto = profilePhoto,
        atmosphere = atmosphere,
        songs = songs.map { it.toDomain() },
        playlistDetailMemorialMedia = toMemorialMedia(),
    )

private fun AfternotePlaylistDto.toMemorialMedia() =
    PlaylistDetailMemorialMedia(
        photoUrl = memorialPhotoUrl ?: profilePhoto,
        videoUrl = memorialVideo?.videoUrl,
        thumbnailUrl = memorialVideo?.thumbnailUrl,
    )

private fun AfternoteDetailReceiverDto.toDomain() =
    DetailReceiver(
        receiverId = receiverId,
        name = name ?: "",
        relation = relation ?: "",
        phone = phone ?: "",
    )

private fun AfternoteSongDto.toDomain() =
    DetailSong(
        id = id,
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )
