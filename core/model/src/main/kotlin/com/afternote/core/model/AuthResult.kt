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
data class UserSession(
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

data class SocialSession(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewUser: Boolean?,
)

/**
 * 토큰 재발급 성공 결과.
 */
data class TokenBundle(
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
