package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteReceiverRef
import com.afternote.feature.afternote.data.dto.request.AfternoteCreateRequest
import com.afternote.feature.afternote.data.dto.request.AfternoteUpdateRequest
import com.afternote.feature.afternote.domain.model.input.CreateGalleryInput
import com.afternote.feature.afternote.domain.model.input.CreatePlaylistInput
import com.afternote.feature.afternote.domain.model.input.CreateSocialInput
import com.afternote.feature.afternote.domain.model.input.UpdateInput

fun UpdateInput.toRequest() =
    AfternoteUpdateRequest(
        category = category,
        title = title,
        processMethod = processMethod,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receivers?.toDto(),
        playlist = playlist?.toDto(),
    )

fun CreateSocialInput.toRequest() =
    AfternoteCreateRequest(
        category = "SOCIAL",
        title = title,
        processMethod = processMethod,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )

fun CreateGalleryInput.toRequest() =
    AfternoteCreateRequest(
        category = "GALLERY",
        title = title,
        processMethod = processMethod,
        actions = actions,
        leaveMessage = leaveMessage,
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )

fun CreatePlaylistInput.toRequest() =
    AfternoteCreateRequest(
        category = "PLAYLIST",
        title = title,
        playlist = playlist.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )
