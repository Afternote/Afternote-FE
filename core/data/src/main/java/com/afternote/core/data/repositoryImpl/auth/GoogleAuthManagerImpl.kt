package com.afternote.core.data.repositoryImpl.auth

import android.app.Activity
import com.afternote.core.domain.repository.auth.GoogleAuthManager
import javax.inject.Inject

/**
 * Google OAuth ID 토큰 제공자.
 *
 * TODO: Credential Manager(`androidx.credentials`) + Google ID Helper 도입 시
 *  [login] 내부에서 실제 Google 로그인 UI를 띄우고 ID 토큰을 반환하도록 구현한다.
 *  현재는 의존성이 없어 항상 실패를 반환한다.
 */
class GoogleAuthManagerImpl
    @Inject
    constructor() : GoogleAuthManager {
        override suspend fun login(activity: Activity): Result<String> =
            Result.failure(UnsupportedOperationException("구글 로그인은 아직 지원되지 않습니다."))
    }
