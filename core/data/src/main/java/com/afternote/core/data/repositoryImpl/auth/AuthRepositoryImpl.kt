package com.afternote.core.data.repositoryImpl.auth

import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.datastore.TokenDataSource
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.repository.auth.GoogleAuthManager
import com.afternote.core.domain.repository.auth.KakaoAuthManager
import com.afternote.core.model.Session
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.dto.LoginRequest
import com.afternote.core.network.dto.LogoutRequest
import com.afternote.core.network.dto.ReissueRequest
import com.afternote.core.network.dto.SocialLoginRequest
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.AuthApiService
import com.afternote.core.network.service.TokenApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Auth와 관련된 에러 정의
sealed class AuthException(
    message: String,
) : Exception(message) {
    class KakaoTokenNotFound : AuthException("카카오 인증 토큰이 없습니다.")

    class GoogleTokenNotFound : AuthException("구글 인증 토큰이 없습니다.")
}

class AuthRepositoryImpl
    @Inject
    constructor(
        private val tokenDataSource: TokenDataSource,
        private val authApiService: AuthApiService,
        private val kakaoAuthManager: KakaoAuthManager,
        private val googleAuthManager: GoogleAuthManager,
        private val tokenApiService: TokenApiService,
    ) : AuthRepository {
        override suspend fun clearSession() = runCatching { tokenDataSource.clearTokens() }

        override suspend fun getAccessToken() = runCatching { tokenDataSource.getAccessToken() }

        override suspend fun getRefreshToken() = runCatching { tokenDataSource.getRefreshToken() }

        override suspend fun saveSession(
            accessToken: String,
            refreshToken: String,
            userId: Long,
        ) = runCatching {
            tokenDataSource.saveTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = userId,
            )
        }

        override val isLoggedIn: Flow<Boolean>
            get() = tokenDataSource.isLoggedIn

        override suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) = runCatching {
            tokenDataSource.updateTokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }

        // TODO:레거시 레포에 있던 authApiService 관련이고 리팩토링해야 하는지 검사 필요

        override suspend fun defaultLogin(
            email: String,
            password: String,
        ): Result<Session.DefaultSession> =
            runCatching {
                val response = authApiService.login(LoginRequest(email, password))
                AuthMapper.toDefaultLoginResult(response.requireData())
            }

        override suspend fun kakaoLogin(): Result<Session.SocialSession> =
            runCatching {
                val socialAccessToken =
                    kakaoAuthManager.getAccessToken() ?: throw AuthException.KakaoTokenNotFound()
                val response =
                    authApiService.socialLogin(
                        SocialLoginRequest(
                            provider = "KAKAO",
                            accessToken = socialAccessToken,
                        ),
                    )
                AuthMapper.toSocialLoginResult(response.requireData())
            }

        override suspend fun googleLogin(): Result<Session.SocialSession> =
            runCatching {
                val socialAccessToken =
                    googleAuthManager.getAccessToken() ?: throw AuthException.GoogleTokenNotFound()
                val response =
                    authApiService.socialLogin(
                        SocialLoginRequest(
                            provider = "GOOGLE",
                            accessToken = socialAccessToken,
                        ),
                    )
                AuthMapper.toSocialLoginResult(response.requireData())
            }

        override suspend fun rotateToken(refreshToken: String): Result<TokenBundle> =
            runCatching {
                val response = tokenApiService.reissue(ReissueRequest(refreshToken))
                val tokenBundleResult = AuthMapper.toRotateTokenResult(response.requireData())
                updateTokens(
                    accessToken = tokenBundleResult.accessToken,
                    refreshToken = tokenBundleResult.refreshToken,
                )
                tokenBundleResult
            }

        override suspend fun logout(refreshToken: String): Result<Unit> =
            runCatching {
                authApiService.logout(LogoutRequest(refreshToken))
            }
    }
