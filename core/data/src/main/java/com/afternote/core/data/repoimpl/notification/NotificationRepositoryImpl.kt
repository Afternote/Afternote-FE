package com.afternote.core.data.repoimpl.notification

import com.afternote.core.domain.repository.notification.NotificationEntry
import com.afternote.core.domain.repository.notification.NotificationInbox
import com.afternote.core.domain.repository.notification.NotificationInboxItem
import com.afternote.core.domain.repository.notification.NotificationRepository
import com.afternote.core.domain.repository.notification.NotificationTarget
import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.NotificationApiService
import javax.inject.Inject

internal class NotificationRepositoryImpl
    @Inject
    constructor(
        private val notificationApiService: NotificationApiService,
    ) : NotificationRepository {
        override suspend fun target(notificationId: Long): Result<NotificationTarget> =
            runCatchingCancellable {
                val dto = notificationApiService.getTarget(notificationId).requireData()
                NotificationTarget(
                    data = dto.data,
                    deleted = dto.deleted,
                    toast = dto.toast?.takeIf(String::isNotBlank),
                )
            }

        override suspend fun markRead(notificationId: Long): Result<Unit> =
            runCatchingCancellable {
                notificationApiService.markRead(notificationId).requireStatus()
            }

        override suspend fun inbox(): Result<NotificationInbox> =
            runCatchingCancellable {
                val dto = notificationApiService.getInbox(page = FIRST_PAGE).requireData()
                NotificationInbox(
                    unreadCount = dto.unreadCount,
                    items =
                        dto.items.map { item ->
                            NotificationInboxItem(
                                id = item.id,
                                title = item.title,
                                body = item.body,
                                data = item.data,
                                isRead = item.readAt != null,
                            )
                        },
                )
            }

        override suspend fun reportEntry(): Result<NotificationEntry> =
            runCatchingCancellable {
                val dto = notificationApiService.reportEntry().requireData()
                NotificationEntry(
                    // 서버가 「보여 주지 마라」 고 한 문구는 들고 다니지 않는다.
                    easyQuestions =
                        if (dto.showEasyQuestions) dto.easyQuestions.filter(String::isNotBlank) else emptyList(),
                    procedureCancelled = dto.procedureCancelled,
                )
            }

        private companion object {
            const val FIRST_PAGE = 0
        }
    }
