package com.afternote.core.domain.error

/**
 * 소셜 로그인 거절(서버 code 1208·1209). [InvalidLoginCredentialsException] 과 갈라 두는 이유 —
 * 입력 필드와 무관한 실패라 화면이 필드 인라인이 아닌 별도 안내로 표시해야 한다.
 * `message` 정책은 [EmailVerificationException] 과 동일(표시 문구는 화면 리소스, BE#92).
 */
class SocialLoginRejectedException(
    cause: Throwable,
) : Exception("social login rejected", cause)
