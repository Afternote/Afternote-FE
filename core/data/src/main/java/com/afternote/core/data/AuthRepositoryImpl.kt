package com.afternote.core.data

import android.util.Log
import com.afternote.core.data.mapper.AuthMapper
import com.afternote.core.datastore.TokenManager
import com.afternote.core.domain.repository.AuthRepository
import com.afternote.core.model.EmailVerifyResult
import com.afternote.core.model.LoginResult
import com.afternote.core.model.ReissueResult
import com.afternote.core.model.SignUpResult
import com.afternote.core.network.AuthApiService
import com.afternote.core.network.dto.LoginRequest
import com.afternote.core.network.dto.LogoutRequest
import com.afternote.core.network.dto.PasswordChangeRequest
import com.afternote.core.network.dto.ReissueRequest
import com.afternote.core.network.dto.SendEmailCodeRequest
import com.afternote.core.network.dto.SignUpRequest
import com.afternote.core.network.dto.SocialLoginRequest
import com.afternote.core.network.dto.VerifyEmailData
import com.afternote.core.network.dto.VerifyEmailRequest
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        val tokenManager: TokenManager,
        val authApiService: AuthApiService,
    ) : AuthRepository {
        override suspend fun clearSession() = tokenManager.clearTokens()

        override suspend fun getAccessToken() = tokenManager.getAccessToken()

        override suspend fun getRefreshToken() = tokenManager.getRefreshToken()

        override suspend fun saveSession(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) = tokenManager.saveTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
        )

        override val isLoggedIn: Flow<Boolean>
            get() = tokenManager.isLoggedInFlow

        override suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) = tokenManager.updateTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
        )

        override suspend fun getUserId() = tokenManager.getUserId()
        // 레거시 레포 기준

        override suspend fun sendEmailCode(email: String): Result<Unit> =
            runCatching {
                Log.d(TAG, "sendEmailCode: email=$email")
                authApiService.sendEmailCode(SendEmailCodeRequest(email))
                Log.d(TAG, "sendEmailCode: SUCCESS")
            }

        override suspend fun verifyEmail(
            email: String,
            certificateCode: String,
        ): Result<EmailVerifyResult> =
            runCatching {
                Log.d(TAG, "verifyEmail: email=$email, code=$certificateCode")
                val response = authApiService.verifyEmail(VerifyEmailRequest(email, certificateCode))
                Log.d(TAG, "verifyEmail: response=$response")
                response.requireStatus()

                AuthMapper.toEmailVerifyResult(response.data ?: VerifyEmailData(isVerified = null))
            }

        override suspend fun signUp(
            email: String,
            password: String,
            name: String,
            profileUrl: String?,
        ): Result<SignUpResult> =
            runCatching {
                Log.d(TAG, "signUp: email=$email, name=$name")
                val response = authApiService.signUp(SignUpRequest(email, password, name, profileUrl))
                Log.d(TAG, "signUp: response=$response")
                AuthMapper.toSignUpResult(response.requireData())
            }

        override suspend fun login(
            email: String,
            password: String,
        ): Result<LoginResult> =
            runCatching {
                Log.d(TAG, "login: email=$email")
                val response = authApiService.login(LoginRequest(email, password))
                Log.d(TAG, "login: response status=${response.status}, message=${response.message}")
                AuthMapper.toLoginResult(response.requireData())
            }

        override suspend fun kakaoLogin(accessToken: String): Result<LoginResult> =
            runCatching {
                val response =
                    authApiService.socialLogin(
                        SocialLoginRequest(
                            provider = "KAKAO",
                            accessToken = accessToken,
                        ),
                    )
                Log.d(
                    TAG,
                    "kakaoLogin: response status=${response.status}, message=${response.message}",
                )
                AuthMapper.toLoginResult(response.requireData())
            }

        override suspend fun reissue(refreshToken: String): Result<ReissueResult> =
            runCatching {
                Log.d(TAG, "reissue: refreshToken=${refreshToken.take(n = 20)}...")
                val response = authApiService.reissue(ReissueRequest(refreshToken))
                Log.d(TAG, "reissue: response=$response")
                AuthMapper.toReissueResult(response.requireData())
            }

        override suspend fun logout(refreshToken: String): Result<Unit> =
            runCatching {
                Log.d(TAG, "logout: refreshToken=${refreshToken.take(n = 20)}...")
                authApiService.logout(LogoutRequest(refreshToken))
                Log.d(TAG, "logout: SUCCESS")
            }

        override suspend fun passwordChange(
            currentPassword: String,
            newPassword: String,
        ): Result<Unit> =
            runCatching {
                Log.d(TAG, "passwordChange: calling API")
                Log.d(TAG, "passwordChange: currentPassword length=${currentPassword.length}")
                Log.d(TAG, "passwordChange: newPassword length=${newPassword.length}")
                val response =
                    authApiService.passwordChange(PasswordChangeRequest(currentPassword, newPassword))
            Log.d(
                TAG,
                "passwordChange: response status=${response.status}, message=${response.message}",
            )
        }

    companion object {
        private const val TAG = "AuthRepositoryImpl"
    }
    }
