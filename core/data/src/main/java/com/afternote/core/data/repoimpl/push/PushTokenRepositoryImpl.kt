package com.afternote.core.data.repoimpl.push

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.push.PushTokenRepository
import com.afternote.core.network.dto.DeletePushTokenRequestDto
import com.afternote.core.network.dto.RegisterPushTokenRequestDto
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.PushTokenApiService
import javax.inject.Inject

internal class PushTokenRepositoryImpl
    @Inject
    constructor(
        private val pushTokenApiService: PushTokenApiService,
    ) : PushTokenRepository {
        override suspend fun register(token: String): Result<Unit> =
            runCatchingCancellable {
                pushTokenApiService
                    .registerPushToken(
                        RegisterPushTokenRequestDto(token = token, platform = ANDROID_PLATFORM),
                    ).requireStatus()
            }

        override suspend fun unregister(token: String): Result<Unit> =
            runCatchingCancellable {
                pushTokenApiService
                    .deletePushToken(DeletePushTokenRequestDto(token = token))
                    .requireStatus()
            }

        private companion object {
            /** 서버 `PushPlatform` enum 의 이름 그대로 보낸다. */
            const val ANDROID_PLATFORM = "ANDROID"
        }
    }
