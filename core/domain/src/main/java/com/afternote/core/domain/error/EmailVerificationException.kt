package com.afternote.core.domain.error

/**
 * 이메일 인증번호가 무효(불일치·만료·미존재 — 서버는 code 1207 하나로 내려준다)라는 사실.
 *
 * HTTP 상태·Retrofit·ApiException 같은 인프라 디테일은 data 계층이 해석한 뒤 이 타입으로 통일하고,
 * presentation 은 타입으로만 분기한다.
 *
 * 서버 문구를 담지 않는 이유 — 이 실패의 표시 문구는 시안이 정했고(`2431:14204`), 서버 `message`
 * 는 사용자 노출용이라는 규정이 명세에 없어 계약으로 삼을 수 없다(BE#92).
 */
class EmailVerificationException(
    cause: Throwable,
) : Exception(null, cause)
