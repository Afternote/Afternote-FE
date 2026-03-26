package com.afternote.core.data.repositoryImpl.auth

import com.afternote.core.data.mapper.auth.AuthMapper
import com.afternote.core.datastore.TokenManager
import com.afternote.core.domain.repository.AuthRepository
import com.afternote.core.model.LoginResult
import com.afternote.core.model.ReissueResult
import com.afternote.core.network.dto.LoginRequest
import com.afternote.core.network.dto.LogoutRequest
import com.afternote.core.network.dto.ReissueRequest
import com.afternote.core.network.dto.SocialLoginRequest
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.AuthApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        val tokenManager: TokenManager,
        val authApiService: AuthApiService,
    ) : AuthRepository {
        // 토큰 매니저 관련
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
        // TODO:레거시 레포에 있던 authApiService 관련이고 리팩토링해야 하는지 검사 필요

        override suspend fun login(
            email: String,
            password: String,
        ): Result<LoginResult> =
            runCatching {
                val response = authApiService.login(LoginRequest(email, password))
                AuthMapper.toLoginResult(response.requireData())
            }

        override suspend fun kakaoLogin(socialAccessToken: String): Result<LoginResult> =
            runCatching {
                val response =
                    authApiService.socialLogin(
                        SocialLoginRequest(
                            provider = "KAKAO",
                            accessToken = socialAccessToken,
                        ),
                    )
                AuthMapper.toLoginResult(response.requireData())
            }

        override suspend fun reissue(refreshToken: String): Result<ReissueResult> =
            runCatching {
                val response = authApiService.reissue(ReissueRequest(refreshToken))
                AuthMapper.toReissueResult(response.requireData())
            }

        override suspend fun logout(refreshToken: String): Result<Unit> =
            runCatching {
                authApiService.logout(LogoutRequest(refreshToken))
            }
    }
