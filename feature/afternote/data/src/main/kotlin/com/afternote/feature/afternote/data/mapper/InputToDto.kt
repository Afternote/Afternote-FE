package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCredentials
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideo
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRef
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload

fun MemorialVideoPayload.toDto() =
    AfternoteMemorialVideo(
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
    )

fun AfternoteAccountCredentials.toDto() =
    AfternoteCredentials(
        id = id,
        password = password,
    )

fun ReceiverRefPayload.toDto() =
    AfternoteReceiverRef(
        receiverId = receiverId,
    )

fun List<ReceiverRefPayload>?.toDto() =
    this?.map {
        it.toDto()
    }
