package com.afternote.feature.afternote.domain.model.author

/**
 * 계정 정보(credentials) 기반 카테고리(SOCIAL·BUSINESS) 공용 생성 페이로드.
 * 두 카테고리는 서버 요청 바디 스키마가 동일하고, category 구분은 data 계층 매퍼가 싣는다.
 */
data class CreateAccountPayload(
    val title: String,
    val processingMethods: List<String>,
    val leaveMessage: String? = null,
    val credentials: AfternoteAccountCredentials? = null,
    val receiverIds: List<Long> = emptyList(),
)
