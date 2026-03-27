package com.afternote.core.data.repositoryImpl.auth

import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.datastore.TokenManager
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.auth.KakaoAuthManager
import com.afternote.core.model.Login
import com.afternote.core.model.RotateToken
import com.afternote.core.model.SocialLogin
import com.afternote.core.network.dto.LoginRequest
import com.afternote.core.network.dto.LogoutRequest
import com.afternote.core.network.dto.ReissueRequest
import com.afternote.core.network.dto.SocialLoginRequest
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.AuthApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Auth와 관련된 에러 정의
sealed class AuthException(
    message: String,
) : Exception(message) {
    class KakaoTokenNotFound : AuthException("카카오 인증 토큰이 없습니다.")
}

class AuthRepositoryImpl
    @Inject
    constructor(
        val tokenManager: TokenManager,
        val authApiService: AuthApiService,
        val kakaoAuthManager: KakaoAuthManager,
    ) : AuthRepository {
        // 토큰 매니저 관련
        override suspend fun clearSession() = runCatching { tokenManager.clearTokens() }

        override suspend fun getAccessToken() = runCatching { tokenManager.getAccessToken() }

        override suspend fun getRefreshToken() = runCatching { tokenManager.getRefreshToken() }

        override suspend fun saveSession(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) = runCatching {
            tokenManager.saveTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = userId,
            )
        }

        override val isLoggedIn: Flow<Boolean>
            get() = tokenManager.isLoggedInFlow

        override suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) = runCatching {
            tokenManager.updateTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }

        override suspend fun getUserId(): Result<Long?> = runCatching { tokenManager.getUserId() }
        // TODO:레거시 레포에 있던 authApiService 관련이고 리팩토링해야 하는지 검사 필요

        override suspend fun login(
            email: String,
            password: String,
        ): Result<Login> =
            runCatching {
                val response = authApiService.login(LoginRequest(email, password))
                AuthMapper.toLoginResult(response.requireData())
            }

        override suspend fun kakaoLogin(): Result<SocialLogin> {
            val socialAccessToken =
                kakaoAuthManager.getAccessToken() ?: throw AuthException.KakaoTokenNotFound()
            return runCatching {
                val response =
                    authApiService.socialLogin(
                        SocialLoginRequest(
                            provider = "KAKAO",
                            accessToken = socialAccessToken,
                        ),
                    )
                AuthMapper.toSocialLoginResult(response.requireData())
            }
        }

        override suspend fun rotateToken(refreshToken: String): Result<RotateToken> =
            runCatching {
                val response = authApiService.reissue(ReissueRequest(refreshToken))
                AuthMapper.toRotateTokenResult(response.requireData())
            }

        override suspend fun logout(refreshToken: String): Result<Unit> =
            runCatching {
                authApiService.logout(LogoutRequest(refreshToken))
            }
    }
