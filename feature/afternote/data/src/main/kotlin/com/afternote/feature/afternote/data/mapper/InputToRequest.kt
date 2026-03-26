package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCreateGalleryRequest
import com.afternote.feature.afternote.data.dto.AfternoteCreatePlaylistRequest
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRef
import com.afternote.feature.afternote.data.dto.AfternoteUpdateRequest
import com.afternote.feature.afternote.data.dto.request.AfternoteCreateSocialRequest
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
    AfternoteCreateSocialRequest(
        category = "SOCIAL",
        title = title,
        processMethod = processMethod,
        actions = actions,
        leaveMessage = leaveMessage,
        credentials = credentials?.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )

fun CreateGalleryInput.toRequest() =
    AfternoteCreateGalleryRequest(
        category = "GALLERY",
        title = title,
        processMethod = processMethod,
        actions = actions,
        leaveMessage = leaveMessage,
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )

fun CreatePlaylistInput.toRequest() =
    AfternoteCreatePlaylistRequest(
        category = "PLAYLIST",
        title = title,
        playlist = playlist.toDto(),
        receivers = receiverIds.map { AfternoteReceiverRef(receiverId = it) },
    )
