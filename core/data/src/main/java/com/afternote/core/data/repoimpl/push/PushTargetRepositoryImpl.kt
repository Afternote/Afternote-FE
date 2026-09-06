package com.afternote.core.data.repoimpl.push

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.push.PushTargetRepository
import com.afternote.core.network.dto.DeletePushTokenRequestDto
import com.afternote.core.network.dto.RegisterPushTokenRequestDto
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.UserApiService
import javax.inject.Inject

/**
 * 도메인의 «푸시 대상 식별자» 와 서버 계약의 «token» 이 만나는 **유일한 지점** (#1570).
 *
 * 서버가 이 값을 `token` 이라 부르므로 DTO·엔드포인트 이름은 그대로 두고, 이름이 갈리는 곳을
 * 여기 한 곳으로 모은다. 코어와 앱은 역할 이름만 보고, 서버 어휘는 이 경계 밖으로 새지 않는다.
 */
internal class PushTargetRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
    ) : PushTargetRepository {
        override suspend fun register(targetId: String): Result<Unit> =
            runCatchingCancellable {
                userApiService
                    .registerPushToken(
                        RegisterPushTokenRequestDto(token = targetId, platform = ANDROID_PLATFORM),
                    ).requireStatus()
            }

        override suspend fun unregister(targetId: String): Result<Unit> =
            runCatchingCancellable {
                userApiService
                    .deletePushToken(DeletePushTokenRequestDto(token = targetId))
                    .requireStatus()
            }

        private companion object {
            /** 서버 `PushPlatform` enum 의 이름 그대로 보낸다. */
            const val ANDROID_PLATFORM = "ANDROID"
        }
    }
