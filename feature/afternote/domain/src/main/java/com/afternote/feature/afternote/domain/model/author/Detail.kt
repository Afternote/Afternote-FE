package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail

/** 애프터노트 상세 도메인 모델. */
data class Detail(
    val id: Long,
    val category: String,
    val title: String,
    val timestamps: DetailTimestamps,
    val type: AfternoteType,
    val credentials: DetailCredentials?,
    val receivers: List<DetailReceiver>,
    /** 사후 처리 방법 선택지 — 서버 `actions` 필드. 화면·폼 어휘("처리 방법")에 맞춘 이름이다. */
    val processingMethods: List<String>,
    val leaveMessageBlocks: List<LeaveMessageBlock>,
    val memorial: MemorialDetail?,
)

data class DetailTimestamps(
    val updatedAt: String,
)

data class DetailCredentials(
    val id: String?,
    val password: String?,
)

/**
 * 애프터노트에 지정된 수신자.
 *
 * 식별자 없는 항목은 매퍼 경계에서 걸러, 도메인부터는 [receiverId] 가 있는 수신자만 다룬다.
 */
data class DetailReceiver(
    val receiverId: Long,
    val name: String,
    val relation: String,
)
