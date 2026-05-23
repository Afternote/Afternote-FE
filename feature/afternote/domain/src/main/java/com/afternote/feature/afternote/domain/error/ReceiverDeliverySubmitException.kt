package com.afternote.feature.afternote.domain.error

/**
 * 수신자 열람 신청(`submitDeliveryVerification`) API 가 거절한 실패.
 *
 * HTTP 상태·Retrofit·BaseResponse 같은 인프라 디테일은 Data 계층에서 해석한 뒤
 * 이 타입으로 통일한다. 도메인·Presentation 은 [serverMessage] 만 받아 사용자에게 노출.
 *
 * @property serverMessage 백엔드가 내려준 사용자 친화 message
 *   (예: 409 `"이미 대기 중인 인증 요청이 존재합니다."`).
 */
class ReceiverDeliverySubmitException(
    val serverMessage: String,
) : Exception(serverMessage)
