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
    /**
     * 임시저장 여부를 바꿀 때만 값을 싣는다. null 이면 서버가 저장값을 유지하므로,
     * 이어쓰기 중 «임시저장으로 계속» 은 null·true 어느 쪽이든 되고 «정식 등록» 만 false 를 명시한다.
     */
    val isDraft: Boolean? = null,
)

data class AfternoteAccountCredentials(
    val id: String? = null,
    val password: String? = null,
)

data class ReceiverRefPayload(
    val receiverId: Long,
)
