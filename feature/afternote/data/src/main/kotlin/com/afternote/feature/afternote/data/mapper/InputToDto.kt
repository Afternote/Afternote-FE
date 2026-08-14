package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCredentialsDto
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideoDto
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRefDto
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload

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

fun List<ReceiverRefPayload>?.toDto() =
    this?.map {
        it.toDto()
    }
