package com.afternote.feature.afternote.data.mapper

import com.afternote.feature.afternote.data.dto.AfternoteCredentials
import com.afternote.feature.afternote.data.dto.AfternoteMemorialVideo
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRef
import com.afternote.feature.afternote.domain.model.CredentialsInput
import com.afternote.feature.afternote.domain.model.ReceiverRefInput
import com.afternote.feature.afternote.domain.model.playlist.MemorialVideoInput
import kotlin.collections.map

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

private fun ReceiverRefInput.toDto() =
    AfternoteReceiverRef(
        receiverId = receiverId,
    )

internal fun List<ReceiverRefInput>?.toDto() =
    this?.map {
        it.toDto()
    }
