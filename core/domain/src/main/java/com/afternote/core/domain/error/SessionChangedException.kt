package com.afternote.core.domain.error

/**
 * 세션에 묶인 작업이 끝나기 전에 그 세션이 끝났거나 다른 세션으로 바뀌었다는 사실 (#2237, #2238).
 *
 * 재발급은 로그인·로그아웃과 락을 공유하지 않는다. 그래서 재발급을 시작한 세션이 응답 도착 전에 사라질 수 있고,
 * 그 응답은 지금 세션에 적용하지 않는다. 이 예외는 "적용하지 않았다" 를 성공과 구분해 알린다.
 */
class SessionChangedException : Exception("session changed before the token rotation was applied")
