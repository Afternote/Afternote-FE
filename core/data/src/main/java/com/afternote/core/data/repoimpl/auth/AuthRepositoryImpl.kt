package com.afternote.core.data.repoimpl.auth

import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.datastore.TokenDataSource
import com.afternote.core.domain.repository.auth.AuthRepository
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

class AuthRepositoryImpl
    @Inject
    constructor(
        private val tokenDataSource: TokenDataSource,
        private val authApiService: AuthApiService,
        private val tokenApiService: TokenApiService,
    ) : AuthRepository {
        override suspend fun clearSession() = runCatching { tokenDataSource.clearTokens() }

        override suspend fun getAccessToken() = runCatching { tokenDataSource.getAccessToken() }

        override suspend fun getRefreshToken() = runCatching { tokenDataSource.getRefreshToken() }

        override suspend fun getUserId() = runCatching { tokenDataSource.getUserId() }

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

        override suspend fun kakaoLogin(oauthToken: String): Result<Session.SocialSession> =
            runCatching {
                val response =
                    authApiService.socialLogin(
                        SocialLoginRequest(
                            provider = "KAKAO",
                            accessToken = oauthToken,
                        ),
                    )
                AuthMapper.toSocialLoginResult(response.requireData())
            }

        override suspend fun googleLogin(idToken: String): Result<Session.SocialSession> =
            runCatching {
                val response =
                    authApiService.socialLogin(
                        SocialLoginRequest(
                            provider = "GOOGLE",
                            accessToken = idToken,
                        ),
                    )
                AuthMapper.toSocialLoginResult(response.requireData())
            }

        override suspend fun rotateToken(): Result<TokenBundle> =
            runCatching {
                val refreshToken =
                    getRefreshToken().getOrNull()
                        ?: error("리프레시 토큰이 존재하지 않습니다.")
                val response = tokenApiService.reissue(ReissueRequest(refreshToken))
                val tokenBundleResult = AuthMapper.toRotateTokenResult(response.requireData())
                updateTokens(
                    accessToken = tokenBundleResult.accessToken,
                    refreshToken = tokenBundleResult.refreshToken,
                ).getOrThrow()
                tokenBundleResult
            }

        override suspend fun logout(): Result<Unit> =
            runCatching {
                val refreshToken =
                    getRefreshToken().getOrNull()
                        ?: error("리프레시 토큰이 존재하지 않습니다.")
                authApiService.logout(LogoutRequest(refreshToken))
            }
    }
