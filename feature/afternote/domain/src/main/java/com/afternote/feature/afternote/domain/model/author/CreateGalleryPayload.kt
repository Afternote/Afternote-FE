package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

data class CreateGalleryPayload(
    val title: String,
    val processingMethods: List<String>,
    val leaveMessageBlocks: List<LeaveMessageBlock>? = null,
    val receiverIds: List<Long> = emptyList(),
)
