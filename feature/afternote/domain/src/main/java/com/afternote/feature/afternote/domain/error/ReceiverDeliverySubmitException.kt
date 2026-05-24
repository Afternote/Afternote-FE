package com.afternote.feature.afternote.domain.error

/**
 * 수신자 열람 신청(`submitDeliveryVerification`) API 가 거절한 실패.
 *
 * HTTP 상태·Retrofit·BaseResponse 같은 인프라 디테일은 Data 계층에서 해석한 뒤
 * 이 타입으로 통일한다. 도메인·Presentation 은 [serverMessage] 만 받아 사용자에게 노출.
 *
 * @property serverMessage 백엔드가 실제로 내려준 사용자 친화 message
 *   (예: 409 `"이미 대기 중인 인증 요청이 존재합니다."`). **null 이면 서버가 message 미제공** —
 *   호출처는 도메인 fallback (정적 R.string) 으로 폴백. 클라가 만든 generic 문구
 *   ("알 수 없는 서버 에러" 등) 는 여기 들어오지 않음.
 * @property httpCode HTTP status code (예: 409). 향후 "특정 code 일 때만 분기" 요구 시 사용.
 *   message 문자열 매칭 회귀 회피.
 */
class ReceiverDeliverySubmitException(
    val serverMessage: String?,
    val httpCode: Int? = null,
) : Exception(serverMessage ?: "submit failed (httpCode=$httpCode)")
