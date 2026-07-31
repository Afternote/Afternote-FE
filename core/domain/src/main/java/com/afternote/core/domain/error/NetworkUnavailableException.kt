package com.afternote.core.domain.error

/**
 * 서버 응답 없이 전송 계층에서 끝난 실패(DNS 해석 불가·타임아웃·연결 거부 등)의 도메인 표현.
 *
 * data 계층이 로그인 등 API 호출의 IO 예외를 이 타입으로 치환한다 — presentation 은
 * core:network 에 의존하지 않으므로, 이 타입 하나로 "네트워크 연결 안내" 분기를 할 수 있다.
 * 원인 예외는 [cause] 로 보존한다(로그 진단용). message 는 의도적으로 null — 기본 생성이면
 * `cause.toString()`(영문 기술 원문)이 message 가 되어, `e.message ?: 폴백` 패턴의 소비처로
 * 원문이 다시 샌다. null 이면 그런 소비처가 각자의 폴백 문구로 안전하게 내려앉는다.
 */
class NetworkUnavailableException(
    cause: Throwable,
) : Exception(null, cause)
