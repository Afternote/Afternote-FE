package com.afternote.feature.afternote.domain.error

/**
 * 수신자가 사망진단서·가족관계증명서를 올려 전달 자격 심사를 신청하는
 * `submitDeliveryVerification` API 가 거절한 실패.
 *
 * 계약(문구·code 의 의미, 왜 흐름별로 나누는지)은 상위 [ReceiverServerRejectionException] 참고.
 *
 * 이 흐름의 `serverMessage` 예시 — 409 `"이미 대기 중인 인증 요청이 존재합니다."`
 */
class ReceiverDeliveryVerificationException(
    status: Int,
    serverMessage: String?,
    serverCode: Int,
) : ReceiverServerRejectionException(
        status = status,
        serverMessage = serverMessage,
        serverMessageFallback = "delivery verification failed (serverCode=$serverCode)",
    )
