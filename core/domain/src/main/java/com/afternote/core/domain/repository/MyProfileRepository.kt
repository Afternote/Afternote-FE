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

    /**
     * 프로필 부분 수정 (`PATCH users/me`).
     *
     * 널은 «이 필드는 건드리지 않는다» 는 뜻이다. 서버 `User.updateProfile`(BE origin/main, 2026-09-04 실측)이
     * 널 필드를 건너뛰고 기존 값을 유지하므로, 바꿀 필드만 채우고 나머지는 널로 둔다. 공용 fake
     * `FakeMyProfileRepository` 도 같은 규칙으로 흉내 낸다.
     *
     * 같은 이유로 이 계약으로는 값을 **지울 수 없다** — [phone]·[profileImageUrl] 은 공백도 널처럼 무시되고,
     * [name] 은 공백이면 서버가 400(INVALID_INPUT_VALUE) 으로 거부한다. 비우기가 필요해지면 서버 계약 변경이 먼저다.
     *
     * @return 수정 뒤의 서버 정본 프로필
     */
    suspend fun updateMyProfile(
        name: String?,
        phone: String?,
        profileImageUrl: String?,
    ): User
}
