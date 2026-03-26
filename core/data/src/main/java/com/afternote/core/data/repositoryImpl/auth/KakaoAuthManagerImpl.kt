package com.afternote.core.data.repositoryImpl.auth

import com.afternote.core.domain.repository.KakaoAuthManager
import com.kakao.sdk.auth.TokenManageable
import javax.inject.Inject

class KakaoAuthManagerImpl
    @Inject
    constructor(
        val tokenManageable: TokenManageable,
    ) : KakaoAuthManager {
        override fun getAccessToken(): String? {
//            @Inject
//            lateinit var tokenManageable: TokenManageable
            return tokenManageable.getToken()?.accessToken
        }
    }
