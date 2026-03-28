package com.afternote.core.model

/**
 * 이메일 인증번호 확인 성공 결과.
 */
data class EmailVerification(
    val isVerified: Boolean,
)

/**
 * 회원가입 성공 결과. (스웨거: userId)
 */
data class AccountRegistration(
    val userId: Long,
    val email: String,
)

/**
 * 로그인 성공 결과.
 */
sealed class Session {
    abstract val accessToken: String
    abstract val refreshToken: String
    abstract val userId: Long

    data class DefaultSession(
        override val accessToken: String,
        override val refreshToken: String,
        override val userId: Long,
    ) : Session()

    data class SocialSession(
        override val accessToken: String,
        override val refreshToken: String,
        override val userId: Long,
        val isNewUser: Boolean?,
    ) : Session()
}

/**
 * 토큰 재발급 성공 결과.
 */
data class TokenBundle(
    val accessToken: String,
    val refreshToken: String,
)
