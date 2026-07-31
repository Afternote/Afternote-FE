package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRefDto
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequestDto
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload

fun AfternoteUpdatePayload.toRequest() =
    AfternoteUpdateRequestDto(
        category = category,
        title = title,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receivers?.toDto(),
        playlist = playlist?.toDto(),
    )

fun CreateAccountPayload.toSocialRequest() =
    AfternoteCreateAccountRequestDto(
        category = "SOCIAL",
        title = title,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
    )

/** BUSINESS 생성 요청. [toSocialRequest] 와 동일 필드 조립이며 category 만 "BUSINESS" 로 실린다. */
fun CreateAccountPayload.toBusinessRequest() =
    AfternoteCreateAccountRequestDto(
        category = "BUSINESS",
        title = title,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
    )

fun CreateGalleryPayload.toRequest() =
    AfternoteCreateGalleryRequestDto(
        category = "GALLERY",
        title = title,
        actions = actions,
        leaveMessage = leaveMessage,
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
    )

fun CreatePlaylistPayload.toRequest() =
    AfternoteCreatePlaylistRequestDto(
        category = "PLAYLIST",
        title = title,
        playlist = playlist.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
    )
