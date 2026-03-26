package com.afternote.core.domain.repository

import com.afternote.core.model.EmailVerifyResult
import com.afternote.core.model.LoginResult
import com.afternote.core.model.ReissueResult
import com.afternote.core.model.SignUpResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
    )

    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun clearSession()

    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun getUserId(): Long?
    // 레거시 레포 기준

    suspend fun sendEmailCode(email: String): Result<Unit>

    suspend fun verifyEmail(
        email: String,
        certificateCode: String,
    ): Result<EmailVerifyResult>

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        profileUrl: String?,
    ): Result<SignUpResult>

    suspend fun login(
        email: String,
        password: String,
    ): Result<LoginResult>

    suspend fun kakaoLogin(accessToken: String): Result<LoginResult>

    suspend fun reissue(refreshToken: String): Result<ReissueResult>

    suspend fun logout(refreshToken: String): Result<Unit>

    suspend fun passwordChange(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit>
}
