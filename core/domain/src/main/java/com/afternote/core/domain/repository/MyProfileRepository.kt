package com.afternote.core.domain.repository

import com.afternote.core.model.user.User

/**
 * 로그인한 사용자 프로필의 서버 정본 조회·수정 계약 (#1282).
 *
 * 서버가 정본인 조회·수정은 여기서만 한다. 홈 진입 placeholder 용 로컬 캐시는
 * [UserProfileCacheRepository] 다.
 */
interface MyProfileRepository {
    // 내 프로필 조회
    suspend fun getMyProfile(): User

    // 프로필 수정
    suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User
}
