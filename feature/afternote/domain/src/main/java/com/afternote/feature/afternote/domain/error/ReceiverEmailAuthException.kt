package com.afternote.feature.afternote.domain.error

/**
 * 수신자 본인 확인 이메일 인증(`receiver-auth/email` 계열) API 가 거절한 실패.
 *
 * 계약(문구·code 의 의미, 왜 흐름별로 나누는지)은 상위 [ReceiverServerRejectionException] 참고.
 *
 * 이 흐름의 `serverCode` 예시 — 1901 = 수신자 이메일 미등록(404 동반),
 * 1902 = 인증번호 만료/미존재(400 동반), 1903 = 인증번호 불일치(400 동반).
 * body 파싱 실패 시 HTTP status 폴백 값이 들어온다.
 * `serverMessage` 예시 — `"등록된 수신자 이메일이 아닙니다."`,
 * `"인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요."`
 */
class ReceiverEmailAuthException(
    status: Int,
    serverMessage: String?,
    serverCode: Int,
) : ReceiverServerRejectionException(
        status = status,
        serverCode = serverCode,
        serverMessage = serverMessage,
        serverMessageFallback = "email auth failed (serverCode=$serverCode)",
    )
