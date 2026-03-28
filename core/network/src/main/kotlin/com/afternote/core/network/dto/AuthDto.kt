package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendEmailCodeRequest(
    val email: String,
)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    @SerialName("certificateCode") val certificateCode: String,
)

@Serializable
data class VerifyEmailData(
    val isVerified: Boolean? = null,
)

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val name: String,
    @SerialName("profileUrl") val profileUrl: String? = null,
)

@Serializable
data class SignUpData(
    @SerialName("userId") val userId: Long,
    val email: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/** Request for unified social login (POST /auth/social/login). */
@Serializable
data class SocialLoginRequest(
    val provider: String,
    @SerialName("accessToken") val accessToken: String,
)

sealed class LoginData {
    abstract val accessToken: String
    abstract val refreshToken: String
    abstract val userId: Long // TODO:백엔드 구현 필요

    @Serializable
    data class DefaultLoginData(
        override val accessToken: String,
        override val refreshToken: String,
        override val userId: Long,
    ) : LoginData()

    @Serializable
    data class SocialLoginData(
        override val accessToken: String,
        override val refreshToken: String,
        override val userId: Long,
        val isNewUser: Boolean? = null,
    ) : LoginData()
}

@Serializable
data class ReissueRequest(
    val refreshToken: String,
)

@Serializable
data class ReissueData(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
)
