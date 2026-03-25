package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCredentials
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideo
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRef
import com.afternote.feature.afternote.domain.model.input.CredentialsInput
import com.afternote.feature.afternote.domain.model.input.MemorialVideoInput
import com.afternote.feature.afternote.domain.model.input.ReceiverRefInput

fun MemorialVideoInput.toDto() =
    AfternoteMemorialVideo(
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
    )

fun CredentialsInput.toDto() =
    AfternoteCredentials(
        id = id,
        password = password,
    )

fun ReceiverRefInput.toDto() =
    AfternoteReceiverRef(
        receiverId = receiverId,
    )

fun List<ReceiverRefInput>?.toDto() =
    this?.map {
        it.toDto()
    }
