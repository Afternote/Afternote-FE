package com.afternote.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 인증(토큰) 관련 도메인 Repository 인터페이스.
 * Presentation/Domain 레이어가 데이터 저장소 구현체에 직접 의존하지 않도록 추상화합니다.
 */
interface AuthRepository {
    /** 로그인 상태를 반응형으로 관찰합니다. */
    val isLoggedInFlow: Flow<Boolean>

    /** 토큰 및 사용자 ID를 저장합니다. */
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: Long,
    )

    /** 저장된 모든 토큰을 삭제합니다. */
    suspend fun clearTokens()

    /** 액세스/리프레시 토큰을 갱신합니다. */
    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String,
    )

    suspend fun getAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun getUserId(): Long?
}
