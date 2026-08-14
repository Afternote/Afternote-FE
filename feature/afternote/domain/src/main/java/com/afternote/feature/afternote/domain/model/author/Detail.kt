package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialDetail

/**
 * 애프터노트 상세 도메인 모델.
 *
 * 서버 GET /api/afternotes/{id} 응답을 도메인 계층으로 매핑한 형태.
 * 카테고리별로 관련 필드만 non-null:
 * - SOCIAL: credentials, processingMethods
 * - GALLERY: receivers, processingMethods
 * - MEMORIAL: memorial
 */
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
    /** 서버 `leaveMessage` 배열 — 값이 없으면 빈 목록이다. */
    val leaveMessageBlocks: List<LeaveMessageBlock>,
    val memorial: MemorialDetail?,
)

data class DetailTimestamps(
    val createdAt: String,
    val updatedAt: String,
)

data class DetailCredentials(
    val id: String?,
    val password: String?,
)

/**
 * 애프터노트에 지정된 수신자.
 *
 * [receiverId] 는 서버 스펙상 필수다(상세 응답의 `ReceiverRequest` 는 이 필드 하나뿐, nullable 표기 없음 —
 * 2026-08-02 Swagger 실측). DTO 는 방어적으로 nullable 이지만 그 경계는 매퍼가 흡수하고,
 * 도메인부터는 식별자가 있는 수신자만 다룬다.
 *
 * name·relation·phone 은 이 응답에 실리지 않아 현재 빈 문자열로 온다 — 서버 확장은 Afternote-BE #81.
 */
data class DetailReceiver(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val phone: String,
)
