package com.afternote.core.domain.error

/**
 * 이메일 인증번호가 무효(불일치·만료·미존재 — 서버는 code 1207 하나로 내려준다)라는 사실.
 *
 * 서버 `message` 를 담지 않는다(BE#92 — 계약이 아니다). 표시 문구는 호출처 리소스가 갖고,
 * `message` 는 리포팅 콘솔용 정적 진단 문자열이며, 코드값은 cause 인 `ApiException` 이 갖는다.
 */
class EmailVerificationException(
    cause: Throwable,
) : Exception("email verification code invalid", cause)
