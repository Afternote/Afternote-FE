package com.afternote.core.data.repoimpl.notification

import com.afternote.core.domain.repository.notification.NotificationRepository
import com.afternote.core.domain.repository.notification.NotificationTarget
import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.UserApiService
import javax.inject.Inject

internal class NotificationRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
    ) : NotificationRepository {
        override suspend fun target(notificationId: Long): Result<NotificationTarget> =
            runCatchingCancellable {
                val dto = userApiService.getNotificationTarget(notificationId).requireData()
                NotificationTarget(
                    destination = dto.data[DESTINATION_KEY],
                    deleted = dto.deleted,
                    toast = dto.toast?.takeIf(String::isNotBlank),
                )
            }

        override suspend fun markRead(notificationId: Long): Result<Unit> =
            runCatchingCancellable {
                userApiService.markNotificationRead(notificationId).requireStatus()
            }

        private companion object {
            const val DESTINATION_KEY = "destination"
        }
    }
