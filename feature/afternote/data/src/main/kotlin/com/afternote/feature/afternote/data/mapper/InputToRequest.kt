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

/**
 * 작성 API의 `category` wire 값. 화면·domain은 [AfternoteType]만 다루고 문자열 변환은 data 경계에서 끝낸다.
 *
 * BUSINESS는 현재 작성 요청 경로가 이미 사용하는 값이다. ESTATE는 저장 미지원이라 이 경계에 도달하면
 * 호출자 버그로 처리한다.
 */
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

/** BUSINESS 생성 요청. [toSocialRequest] 와 동일 필드 조립이며 wire `category`만 "BUSINESS" 로 실린다. */
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
