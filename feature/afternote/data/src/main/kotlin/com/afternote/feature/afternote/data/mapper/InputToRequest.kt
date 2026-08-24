package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequestDto
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRefDto
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequestDto
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload

fun AfternoteUpdatePayload.toRequest() =
    AfternoteUpdateRequestDto(
        category = type.toAuthoringServerCategory(),
        title = title,
        processingMethods = processingMethods,
        leaveMessage = leaveMessageBlocks.toDto(),
        credentials = credentials?.toDto(),
        receivers = receivers?.toDto(),
        memorial = memorial?.toDto(),
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
    )

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
