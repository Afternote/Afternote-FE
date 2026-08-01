package com.afternote.feature.afternote.domain.error

/**
 * 수신자 본인 확인 이메일 인증(`receiver-auth/email` 계열) API 가 거절한 실패.
 *
 * HTTP 상태·Retrofit·BaseResponse 같은 인프라 디테일은 Data 계층에서 해석한 뒤
 * 이 타입으로 통일한다. 도메인·Presentation 은 [serverMessage] 만 받아 사용자에게 노출.
 * ([ReceiverDeliverySubmitException] 과 같은 패턴 — 흐름별 예외를 분리해 호출처가 출처를 구분.)
 *
 * @property serverMessage 백엔드가 실제로 내려준 사용자 친화 message
 *   (예: `"등록된 수신자 이메일이 아닙니다."`, `"인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요."`).
 *   **null 이면 서버가 message 미제공** — 호출처는 도메인 fallback (정적 R.string) 으로 폴백.
 * @property serverCode 서버 에러 body 의 `code` (예: 1901 = 수신자 이메일 미등록(404 동반),
 *   1902 = 인증번호 만료/미존재(400 동반), 1903 = 인증번호 불일치(400 동반)).
 *   body 파싱 실패 시 HTTP status 폴백 값이 들어온다.
 *   향후 "특정 code 일 때만 분기" 요구 시 사용 — message 문자열 매칭 회귀 회피.
 */
class ReceiverEmailAuthException(
    val serverMessage: String?,
    val serverCode: Int? = null,
) : Exception(serverMessage ?: "email auth failed (serverCode=$serverCode)")
