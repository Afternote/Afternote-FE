package com.afternote.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * 사용자 프로필 로컬 캐시 도메인 진입점.
 *
 * 홈 진입 시 `GET /users/me` 응답이 도착하기 전 placeholder 로 즉시 노출하기 위한 값
 * (사용자 이름·패스키 등록 여부)을 담는다.
 *
 * ViewModel 은 datastore DataSource 를 직접 참조하지 않고 본 인터페이스를 통해서만 접근한다
 * (UI → Domain → Data 단일 진입점).
 */
interface UserProfileCacheRepository {
    fun isPasskeyRegisteredFlow(): Flow<Boolean>

    suspend fun savePasskeyRegistered(registered: Boolean)

    suspend fun getCachedUserName(): String?

    suspend fun saveUserName(name: String)
}
