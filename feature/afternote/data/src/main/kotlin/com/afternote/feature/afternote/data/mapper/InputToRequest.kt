package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCreateAccountRequest
import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequest
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequest
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRef
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequest
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload

fun AfternoteUpdatePayload.toRequest() =
    AfternoteUpdateRequest(
        category = category,
        title = title,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receivers?.toDto(),
        playlist = playlist?.toDto(),
    )

fun CreateAccountPayload.toSocialRequest() =
    AfternoteCreateAccountRequest(
        category = "SOCIAL",
        title = title,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )

/** BUSINESS 생성 요청. [toSocialRequest] 와 동일 필드 조립이며 category 만 "BUSINESS" 로 실린다. */
fun CreateAccountPayload.toBusinessRequest() =
    AfternoteCreateAccountRequest(
        category = "BUSINESS",
        title = title,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )

fun CreateGalleryPayload.toRequest() =
    AfternoteCreateGalleryRequest(
        category = "GALLERY",
        title = title,
        actions = actions,
        leaveMessage = leaveMessage,
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )

fun CreatePlaylistPayload.toRequest() =
    AfternoteCreatePlaylistRequest(
        category = "PLAYLIST",
        title = title,
        playlist = playlist.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )
