package com.afternote.feature.onboarding.data.repositoryimpl

import com.afternote.core.datastore.TokenManager
import com.afternote.core.network.model.requireData
import com.afternote.feature.onboarding.data.dto.request.KakaoLoginRequest
import com.afternote.feature.onboarding.data.service.LoginApiService
import com.afternote.feature.onboarding.domain.repository.LoginRepository
import javax.inject.Inject

/**
 * LoginRepository 구현체.
 * Kakao 액세스 토큰을 서버에 전달해 앱 토큰을 받아 TokenManager 에 저장합니다.
 */
class LoginRepositoryImpl
    @Inject
    constructor(
        private val loginApiService: LoginApiService,
        private val tokenManager: TokenManager,
    ) : LoginRepository {
        override suspend fun loginWithKakao(kakaoAccessToken: String): Result<Unit> =
            runCatching {
                val response = loginApiService.loginWithKakao(KakaoLoginRequest(kakaoAccessToken))
                val data = response.requireData()
                tokenManager.saveTokens(
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken,
                    userId = data.userId,
                )
            }
    }
