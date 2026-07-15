package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendEmailCodeRequestDto(
    val email: String,
)

@Serializable
data class VerifyEmailRequestDto(
    val email: String,
    @SerialName("certificateCode") val certificateCode: String,
)

@Serializable
data class SignUpRequestDto(
    val email: String,
    val password: String,
    val name: String,
    @SerialName("profileUrl") val profileUrl: String? = null,
)

@Serializable
data class SignUpDto(
    @SerialName("userId") val userId: Long,
    val email: String,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

/** Request for unified social login (POST /auth/social/login). */
@Serializable
data class SocialLoginRequestDto(
    val provider: String,
    @SerialName("accessToken") val accessToken: String,
)

sealed class LoginDto {
    abstract val accessToken: String
    abstract val refreshToken: String

    /**
     * 액세스 토큰 잔여 수명(초). BE #410(2026-06-20)으로 발급 응답 `data` 안에 포함 —
     * RFC 6749 §5.1 의 `expires_in` 자리(access_token 형제)다. 선제 reissue(#408) deadline 의
     * 입력값으로, 토큰 저장 시점에 `AuthRepositoryImpl` 이 `AccessTokenExpiryTracker.record` 로
     * 기록한다. 서버가 생략하면(과거 호환) null — 그땐 기존 deadline 을 비워(stale 방지)
     * 401 사후 대응이 받는다.
     */
    abstract val expiresIn: Long?

    @Serializable
    data class DefaultLoginDto(
        override val accessToken: String,
        override val refreshToken: String,
        override val expiresIn: Long? = null,
    ) : LoginDto()

    @Serializable
    data class SocialLoginDto(
        override val accessToken: String,
        override val refreshToken: String,
        @SerialName("newUser") val isNewUser: Boolean? = null,
        override val expiresIn: Long? = null,
    ) : LoginDto()
}

@Serializable
data class ReissueRequestDto(
    val refreshToken: String,
)

@Serializable
data class ReissueDto(
    val accessToken: String,
    val refreshToken: String,
    /** 재발급된 액세스 토큰의 잔여 수명(초). BE #410 으로 reissue 응답 `data` 에 포함. [LoginDto.expiresIn] 참고. */
    val expiresIn: Long? = null,
)

@Serializable
data class LogoutRequestDto(
    val refreshToken: String,
)

@Serializable
data class PasswordChangeRequestDto(
    val currentPassword: String,
    val newPassword: String,
)
