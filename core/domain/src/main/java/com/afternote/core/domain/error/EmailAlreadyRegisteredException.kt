package com.afternote.core.domain.error

/**
 * 이미 가입된 이메일로 인증코드 발송을 요청했다는 사실(서버 code 1200).
 *
 * 서버 `message` 를 담지 않는다(BE#92 — 계약이 아니다). 표시 문구는 호출처 리소스가 갖고,
 * `message` 는 리포팅 콘솔용 정적 진단 문자열이며, 코드값은 cause 인 `ApiException` 이 갖는다.
 */
class EmailAlreadyRegisteredException(
    cause: Throwable,
) : Exception("email already registered", cause)
