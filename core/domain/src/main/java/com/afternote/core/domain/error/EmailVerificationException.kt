package com.afternote.core.domain.error

/**
 * 이메일 인증번호가 무효(불일치·만료·미존재 — 서버는 code 1207 하나로 내려준다)라는 사실.
 *
 * HTTP 상태·Retrofit·ApiException 같은 인프라 디테일은 data 계층이 해석한 뒤 이 타입으로 통일하고,
 * presentation 은 타입으로만 분기한다.
 *
 * 서버 문구를 담지 않는 이유 — 이 실패의 표시 문구는 시안이 정했고(`2431:14204`), 서버 `message`
 * 는 사용자 노출용이라는 규정이 명세에 없어 계약으로 삼을 수 없다(BE#92).
 *
 * `message` 에 넣은 것은 **표시용이 아니라 정적 진단 문자열**이다. 이 예외는 크래시 리포팅
 * (`recordAuthFailure`) 경로에 오르는데, null 로 두면 콘솔에 타입만 남아 cause 체인을 한 단계
 * 더 들어가야 사유가 보인다. 서버가 주는 값이 아니라 코드에 박힌 상수라 계약 문제도 없다.
 *
 * 서버 code 숫자는 담지 않는다 — 판정 상수는 `AccountFailureMapper` 한 곳에만 두고, 실제 코드값은
 * cause 인 `ApiException` 이 갖고 있어 콘솔의 cause 체인에 그대로 찍힌다.
 */
class EmailVerificationException(
    cause: Throwable,
) : Exception("email verification code invalid", cause)
