package com.afternote.core.data.repoimpl

import com.afternote.core.data.mapper.user.toDomain
import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.model.user.User
import com.afternote.core.network.dto.UserUpdateProfileRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.UserApiService
import javax.inject.Inject

/**
 * 서버 정본 프로필 조회·수정 구현 (#1282).
 *
 * 상태를 갖지 않고 [UserApiService] 위임과 매퍼가 전부다 — 세션·캐시는 수신자 계약 전용이다.
 * 로컬 캐시(사용자 이름·패스키 등록 여부)는 별개 책임이라 `UserProfileRepositoryImpl` 이 담당한다.
 * 상태가 없으므로 스코프를 두지 않는다 — 인스턴스가 몇 개든 관찰되는 동작이 같다.
 */
internal class MyProfileRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
    ) : MyProfileRepository {
        override suspend fun getMyProfile(): User =
            userApiService
                .getMyProfile()
                .requireData()
                .toDomain()

        override suspend fun updateMyProfile(
            name: String?,
            phone: String?,
            profileImageUrl: String?,
        ): User =
            userApiService
                .updateMyProfile(
                    UserUpdateProfileRequestDto(
                        name = name,
                        phone = phone,
                        profileImageUrl = profileImageUrl,
                    ),
                ).requireData()
                .toDomain()
    }
