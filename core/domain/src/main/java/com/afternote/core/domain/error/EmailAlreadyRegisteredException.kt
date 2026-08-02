package com.afternote.core.domain.error

/**
 * 이미 가입된 이메일로 인증코드 발송을 요청했다는 사실(서버 code 1200).
 *
 * 서버 `message` 를 담지 않는다 — 그 필드가 사용자 노출용이라는 규정이 명세에 없고(BE#92 에서
 * 확인해 요청을 거뒀다), 계약이 아닌 값은 언제든 바뀔 수 있다. 계약인 것은 `code` 뿐이라
 * code 로만 갈라내고 표시 문구는 호출처가 자기 리소스로 갖는다.
 */
class EmailAlreadyRegisteredException(
    cause: Throwable,
) : Exception(null, cause)
