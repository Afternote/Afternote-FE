package com.afternote.core.domain.repository.auth

/**
 * 사용자가 소셜 로그인(카카오, 구글 등)을 직접 취소했을 때 발생하는 예외.
 *
 * 코루틴의 [java.util.concurrent.CancellationException]과 구분하여
 * 구조화된 동시성(Structured Concurrency)이 깨지지 않도록 별도 타입으로 정의한다.
 */
class UserCancelledAuthException(
    message: String = "사용자가 로그인을 취소했습니다.",
) : Exception(message)
