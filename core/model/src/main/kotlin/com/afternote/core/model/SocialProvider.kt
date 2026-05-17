package com.afternote.core.model

/**
 * 소셜 로그인·계정 연동에 사용되는 OAuth 제공자.
 *
 * `local` (이메일+비밀번호 계정) 은 path param 대상이 아니라 enum 에서 제외.
 *
 * 표현 형식이 endpoint 마다 달라 변환 함수를 분리:
 * - [toPath]: `/users/connected-accounts/{provider}` 같은 URL path 용 (lowercase)
 * - [toBody]: `SocialLoginRequest.provider` 같은 request body 용 (uppercase)
 */
enum class SocialProvider {
    GOOGLE,
    NAVER,
    KAKAO,
    APPLE,
    ;

    fun toPath(): String = name.lowercase()

    fun toBody(): String = name
}
