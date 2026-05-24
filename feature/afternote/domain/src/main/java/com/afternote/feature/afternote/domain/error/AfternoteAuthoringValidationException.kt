package com.afternote.feature.afternote.domain.error

/**
 * 저장(생성/수정) API가 거절한 검증 실패.
 * HTTP 상태·Retrofit·에러 바디 형식은 Data 계층에서 해석한 뒤 이 타입으로 통일한다.
 */
enum class AfternoteAuthoringValidationKind {
    /** 서버 코드 475 — 수신자 최소 1명 필요. */
    RECEIVERS_REQUIRED,
}

class AfternoteAuthoringValidationException(
    val kind: AfternoteAuthoringValidationKind,
) : Exception("Afternote authoring validation: ${kind.name}")
