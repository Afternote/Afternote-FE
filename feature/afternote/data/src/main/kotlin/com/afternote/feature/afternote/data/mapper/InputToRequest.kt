package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRefDto
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequestDto
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload

fun AfternoteUpdatePayload.toRequest() =
    AfternoteUpdateRequestDto(
        category = category,
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        credentials = credentials?.toDto(),
        receivers = receivers?.toDto(),
        memorial = memorial?.toDto(),
    )

fun CreateAccountPayload.toSocialRequest() =
    AfternoteCreateAccountRequestDto(
        category = "SOCIAL",
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
    )

/** BUSINESS 생성 요청. [toSocialRequest] 와 동일 필드 조립이며 category 만 "BUSINESS" 로 실린다. */
fun CreateAccountPayload.toBusinessRequest() =
    AfternoteCreateAccountRequestDto(
        category = "BUSINESS",
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
    )

fun CreateGalleryPayload.toRequest() =
    AfternoteCreateGalleryRequestDto(
        category = "GALLERY",
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
    )

fun CreateMemorialPayload.toRequest() =
    AfternoteCreatePlaylistRequestDto(
        category = "PLAYLIST",
        title = title,
        memorial = memorial.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRefDto(receiverId = it) },
    )
