package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCredentialsDto
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideoDto
import com.afternote.feature.afternote.data.dto.AfternotePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRefDto
import com.afternote.feature.afternote.data.dto.AfternoteSongDto
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequestDto
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload

/**
 * 도메인 → 요청 wire. 요청 바디 루트는 `toRequest`, 그 안에 실리는 부분 DTO 는 `toDto` 다.
 */

fun AfternoteUpdatePayload.toRequest() =
    AfternoteUpdateRequestDto(
        category = type.toAuthoringServerCategory(),
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        credentials = credentials?.toDto(),
        receivers = receivers?.map { it.toDto() },
        memorial = memorial?.toDto(),
        isDraft = isDraft,
    )

/** 문자열 변환은 data 경계에서 끝낸다 — 화면·domain 은 [AfternoteType] 만 다룬다. */
private fun AfternoteType.toAuthoringServerCategory(): String =
    when (this) {
        AfternoteType.SOCIAL_NETWORK -> "SOCIAL"
        AfternoteType.BUSINESS -> "BUSINESS"
        AfternoteType.GALLERY_AND_FILES -> "GALLERY"
        AfternoteType.MEMORIAL -> "PLAYLIST"
        AfternoteType.ESTATE -> error("Unsupported authoring type: $this")
    }

fun CreateAccountPayload.toSocialRequest() =
    AfternoteCreateAccountRequestDto(
        category = "SOCIAL",
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
        isDraft = isDraft,
    )

fun CreateAccountPayload.toBusinessRequest() =
    AfternoteCreateAccountRequestDto(
        category = "BUSINESS",
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
        isDraft = isDraft,
    )

fun CreateGalleryPayload.toRequest() =
    AfternoteCreateGalleryRequestDto(
        category = "GALLERY",
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
        isDraft = isDraft,
    )

fun CreateMemorialPayload.toRequest() =
    AfternoteCreatePlaylistRequestDto(
        category = "PLAYLIST",
        title = title,
        memorial = memorial.toDto(),
        leaveMessage = leaveMessageBlocks.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
        isDraft = isDraft,
    )

fun MemorialWritePayload.toDto() =
    AfternotePlaylistRequestDto(
        memorialPhotoUrl = memorialPhotoUrl,
        songs = songs.map { it.toDto() },
        memorialVideo = memorialVideo?.toDto(),
    )

fun MemorialSongPayload.toDto() =
    AfternoteSongDto(
        title = title,
        artist = artist,
        coverUrl = coverUrl,
    )

fun MemorialVideoPayload.toDto() =
    AfternoteMemorialVideoDto(
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
    )

fun AfternoteAccountCredentials.toDto() =
    AfternoteCredentialsDto(
        id = id,
        password = password,
    )

fun ReceiverRefPayload.toDto() =
    AfternoteReceiverRefDto(
        receiverId = receiverId,
    )
