package com.afternote.core.domain.error

/**
 * 이메일 로그인 자격 거절(서버 code 1201·1202). 계정 미존재와 비밀번호 불일치를 가르지 않는다 —
 * 서버 문구도 시안(`3628:23437`)도 어느 쪽이 틀렸는지 노출하지 않는 단일 문구다.
 * `message` 정책은 [EmailVerificationException] 과 동일(표시 문구는 화면 리소스, BE#92).
 */
class InvalidLoginCredentialsException(
    cause: Throwable,
) : Exception("login credentials rejected", cause)
