package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

data class AfternoteUpdatePayload(
    val type: AfternoteType,
    val title: String,
    val processingMethods: List<String>? = null,
    val leaveMessageBlocks: List<LeaveMessageBlock> = emptyList(),
    val credentials: AfternoteAccountCredentials? = null,
    val receivers: List<ReceiverRefPayload>? = null,
    val memorial: MemorialWritePayload? = null,
)

data class AfternoteAccountCredentials(
    val id: String? = null,
    val password: String? = null,
)

data class ReceiverRefPayload(
    val receiverId: Long,
)
