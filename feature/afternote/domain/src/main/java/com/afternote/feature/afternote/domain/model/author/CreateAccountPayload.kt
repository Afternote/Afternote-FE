package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

/**
 * 계정 정보(credentials) 기반 카테고리(SOCIAL·BUSINESS) 공용 생성 페이로드.
 * 두 카테고리는 서버 요청 바디 스키마가 동일하고, category 구분은 data 계층 매퍼가 싣는다.
 */
data class CreateAccountPayload(
    val title: String,
    val processingMethods: List<String>,
    val leaveMessageBlocks: List<LeaveMessageBlock> = emptyList(),
    val credentials: AfternoteAccountCredentials? = null,
    val receiverIds: List<Long> = emptyList(),
    /** true 면 임시저장으로 만든다 — 서버가 카테고리별 필수값 검증을 건너뛴다(BE `AfternoteValidator`). */
    val isDraft: Boolean = false,
)
