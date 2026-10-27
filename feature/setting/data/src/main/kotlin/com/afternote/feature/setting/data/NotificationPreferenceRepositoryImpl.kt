package com.afternote.feature.setting.data

import com.afternote.core.domain.result.runCatchingCancellable
import com.afternote.core.network.dto.NotificationAnniversaryDto
import com.afternote.core.network.dto.NotificationAnniversaryRequestDto
import com.afternote.core.network.dto.NotificationDelegateRequestDto
import com.afternote.core.network.dto.NotificationPreferencesDto
import com.afternote.core.network.dto.NotificationPreferencesRequestDto
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.core.network.service.NotificationApiService
import com.afternote.feature.setting.domain.NotificationAnniversary
import com.afternote.feature.setting.domain.NotificationDelegate
import com.afternote.feature.setting.domain.NotificationPreferenceRepository
import com.afternote.feature.setting.domain.NotificationPreferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

internal class NotificationPreferenceRepositoryImpl
    @Inject
    constructor(
        private val notificationApiService: NotificationApiService,
    ) : NotificationPreferenceRepository {
        override suspend fun getPreferences(): Result<NotificationPreferences> =
            runCatchingCancellable { notificationApiService.getPreferences().requireData().toDomain() }

        override suspend fun updatePreferences(preferences: NotificationPreferences): Result<NotificationPreferences> =
            runCatchingCancellable {
                notificationApiService
                    .updatePreferences(
                        NotificationPreferencesRequestDto(
                            enabled = preferences.enabled,
                            time = preferences.time.format(WIRE_TIME),
                            weekdays = preferences.weekdays.sorted().map(DayOfWeek::name),
                            weeklyGoal = preferences.weeklyGoal,
                        ),
                    ).requireData()
                    .toDomain()
            }

        override suspend fun getDelegate(): Result<NotificationDelegate?> =
            runCatchingCancellable {
                val dto = notificationApiService.getDelegate().requireData()
                val name = dto.name?.takeIf(String::isNotBlank)
                val phone = dto.phone?.takeIf(String::isNotBlank)
                if (name != null && phone != null) NotificationDelegate(name = name, phone = phone) else null
            }

        override suspend fun updateDelegate(delegate: NotificationDelegate): Result<Unit> =
            runCatchingCancellable {
                notificationApiService
                    .updateDelegate(NotificationDelegateRequestDto(name = delegate.name, phone = delegate.phone))
                    .requireStatus()
            }

        override suspend fun deleteDelegate(): Result<Unit> =
            runCatchingCancellable { notificationApiService.deleteDelegate().requireStatus() }

        override suspend fun getAnniversaries(): Result<List<NotificationAnniversary>> =
            runCatchingCancellable { notificationApiService.getAnniversaries().requireData().map { it.toDomain() } }

        override suspend fun addAnniversary(
            receiverId: Long,
            month: Int,
            day: Int,
        ): Result<NotificationAnniversary> =
            runCatchingCancellable {
                notificationApiService
                    .createAnniversary(NotificationAnniversaryRequestDto(receiverId = receiverId, month = month, day = day))
                    .requireData()
                    .toDomain()
            }

        override suspend fun deleteAnniversary(anniversaryId: Long): Result<Unit> =
            runCatchingCancellable { notificationApiService.deleteAnniversary(anniversaryId).requireStatus() }

        private companion object {
            val WIRE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        }
    }

private fun NotificationPreferencesDto.toDomain(): NotificationPreferences =
    NotificationPreferences(
        enabled = enabled,
        time = time.toLocalTime(),
        // 모르는 요일 이름은 버린다 — 서버가 enum 을 늘려도 화면은 아는 요일만 그린다.
        weekdays = weekdays.mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name } }.toSet(),
        weeklyGoal = weeklyGoal,
    )

private fun NotificationAnniversaryDto.toDomain(): NotificationAnniversary =
    NotificationAnniversary(id = id, receiverId = receiverId, month = month, day = day)

/**
 * 서버 `LocalTime` 을 읽는다. Jackson 기본 설정이면 `"18:00:00"` 문자열이고, 타임스탬프 모드면
 * `[18,0]` 배열이다. 둘 다 아니면 계약 위반이라 던진다 — 기본값으로 메우면 사용자가 고른 시각이
 * 18:00 으로 보이는 거짓이 된다.
 */
private fun JsonElement.toLocalTime(): LocalTime =
    when (this) {
        is JsonPrimitive -> {
            LocalTime.parse(content)
        }

        is JsonArray -> {
            LocalTime.of(this[0].jsonPrimitive.int, getOrNull(1)?.jsonPrimitive?.int ?: 0)
        }

        else -> {
            throw IllegalArgumentException("알림 시각 형식을 읽을 수 없다: $this")
        }
    }
