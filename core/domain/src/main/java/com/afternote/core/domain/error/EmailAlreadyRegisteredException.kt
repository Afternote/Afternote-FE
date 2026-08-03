package com.afternote.core.domain.error

/**
 * 이미 가입된 이메일로 인증코드 발송을 요청했다는 사실(서버 code 1200).
 *
 * 서버 `message` 를 담지 않는다 — 그 필드가 사용자 노출용이라는 규정이 명세에 없고(BE#92 에서
 * 확인해 요청을 거뒀다), 계약이 아닌 값은 언제든 바뀔 수 있다. 계약인 것은 `code` 뿐이라
 * code 로만 갈라내고 표시 문구는 호출처가 자기 리소스로 갖는다.
 *
 * `message` 에 넣은 것은 **표시용이 아니라 정적 진단 문자열**이다. 이 예외는 크래시 리포팅
 * (`recordAuthFailure(EMAIL_CODE_SEND, ...)`) 경로에 오르는데, null 로 두면 콘솔에 타입만 남아
 * cause 체인을 한 단계 더 들어가야 사유가 보인다. 서버가 주는 값이 아니라 코드에 박힌 상수다.
 *
 * 서버 code 숫자는 담지 않는다 — 판정 상수는 `AccountFailureMapper` 한 곳에만 두고, 실제 코드값은
 * cause 인 `ApiException` 이 갖고 있어 콘솔의 cause 체인에 그대로 찍힌다.
 */
class EmailAlreadyRegisteredException(
    cause: Throwable,
) : Exception("email already registered", cause)
