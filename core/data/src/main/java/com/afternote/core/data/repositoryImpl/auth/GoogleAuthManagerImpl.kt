package com.afternote.core.data.repositoryImpl.auth

import com.afternote.core.domain.repository.auth.GoogleAuthManager
import javax.inject.Inject

/**
 * Google OAuth 액세스 토큰 제공자.
 *
 * 카카오의 [KakaoAuthManagerImpl] 와 동일한 위치/역할.
 * 실제 Google 인증 SDK(Credential Manager 등) 연결 전까지는
 * 토큰 소스가 없어 항상 null을 반환한다 — [AuthRepositoryImpl.googleLogin] 에서
 * [AuthException.GoogleTokenNotFound] 로 변환된다.
 *
 * TODO: Credential Manager(`androidx.credentials`) + Google ID Helper 도입 시
 *  토큰 캐시/리프레시 로직을 이 구현체에 추가한다.
 */
class GoogleAuthManagerImpl
    @Inject
    constructor() : GoogleAuthManager {
        override fun getAccessToken(): String? = null
    }
