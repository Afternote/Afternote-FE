package com.afternote.core.domain.error

/**
 * 회원가입 이메일 인증(`auth/email/verify`) API 가 거절한 실패.
 *
 * 서버는 인증번호 불일치·만료·미존재를 code 1207(인증번호 무효) 하나로 내려준다.
 * HTTP 상태·Retrofit·ApiException 같은 인프라 디테일은 Data 계층에서 해석한 뒤
 * 이 타입으로 통일한다 — Presentation 은 타입으로만 분기한다.
 * (feature:afternote 의 ReceiverEmailAuthException 과 같은 패턴 — 흐름별 예외를 분리해
 * 호출처가 출처를 구분.)
 *
 * @property serverMessage 백엔드가 실제로 내려준 사용자 친화 message
 *   (예: `"인증번호가 유효하지 않습니다."`). **null 이면 서버가 message 미제공** —
 *   호출처는 도메인 fallback (정적 R.string) 으로 폴백.
 * @property serverCode 서버 에러 body 의 `code` (1207 = 인증번호 무효 — 불일치/만료/미존재 통합).
 *   향후 "특정 code 일 때만 분기" 요구 시 사용 — message 문자열 매칭 회귀 회피.
 *
 * 헤더의 `Exception(serverMessage ?: ...)` 는 부모 생성자 위임(Java 의 `super(message)`) —
 * `Throwable.message` 는 생성자에서만 설정 가능하므로 여기가 message 를 확정할 유일한 자리다.
 * 매 인스턴스 생성마다 엘비스가 평가되어, serverMessage 부재 시 serverCode 를 보간한 진단
 * 문자열로 폴백 — message 가 null 인 예외(#475 무음 실패의 원인 유형)를 태생부터 차단한다.
 */
class EmailVerificationException(
    val serverMessage: String?,
    val serverCode: Int? = null,
) : Exception(serverMessage ?: "email verification failed (serverCode=$serverCode)")
