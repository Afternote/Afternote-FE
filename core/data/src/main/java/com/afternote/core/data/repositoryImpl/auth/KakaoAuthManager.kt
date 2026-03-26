package com.afternote.core.data.repositoryImpl.auth

// import com.kakao.sdk.auth.TokenManagerProvider
import com.afternote.core.domain.repository.KakaoAuthManager
import com.afternote.core.network.service.AuthApiService
import javax.inject.Inject

class KakaoAuthManagerImpl
    @Inject
    constructor(
        val authApiService: AuthApiService,
    ) : KakaoAuthManager {
        override fun getAccessToken(): String? {
//            val token = TokenManagerProvider.instance.manager.getToken()
        }
    }
