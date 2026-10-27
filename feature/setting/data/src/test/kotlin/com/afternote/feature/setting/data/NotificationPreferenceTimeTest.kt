package com.afternote.feature.setting.data

import com.afternote.core.network.dto.NotificationAnniversaryDto
import com.afternote.core.network.dto.NotificationAnniversaryRequestDto
import com.afternote.core.network.dto.NotificationDelegateDto
import com.afternote.core.network.dto.NotificationDelegateRequestDto
import com.afternote.core.network.dto.NotificationEntryDto
import com.afternote.core.network.dto.NotificationInboxDto
import com.afternote.core.network.dto.NotificationPreferencesDto
import com.afternote.core.network.dto.NotificationPreferencesRequestDto
import com.afternote.core.network.dto.NotificationTargetDto
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.service.NotificationApiService
import com.afternote.feature.setting.domain.NotificationPreferences
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * 서버 알림 설정의 와이어 변환 (#2321).
 *
 * 서버 `LocalTime` 은 Jackson 설정에 따라 문자열이나 배열로 온다 — 어느 쪽이든 같은 시각이어야 하고,
 * 둘 다 아니면 기본값으로 메우지 않고 실패해야 한다. 보낼 때는 `HH:mm` 과 요일 이름이다.
 */
class NotificationPreferenceTimeTest {
    @Test
    fun `문자열과 배열 시각을 같은 값으로 읽는다`() =
        runTest {
            listOf("\"18:00:00\"", "\"18:00\"", "[18,0]", "[18,0,0]").forEach { wire ->
                val repository = NotificationPreferenceRepositoryImpl(FakeService(time = Json.parseToJsonElement(wire)))

                assertEquals(wire, LocalTime.of(18, 0), repository.getPreferences().getOrThrow().time)
            }
        }

    @Test
    fun `알 수 없는 시각 형식은 기본값으로 메우지 않고 실패한다`() =
        runTest {
            val repository = NotificationPreferenceRepositoryImpl(FakeService(time = JsonObject(emptyMap())))

            assertTrue(repository.getPreferences().isFailure)
        }

    @Test
    fun `저장은 HH-mm 과 요일 이름으로 보낸다`() =
        runTest {
            val service = FakeService(time = Json.parseToJsonElement("\"07:05:00\""))
            val repository = NotificationPreferenceRepositoryImpl(service)

            repository.updatePreferences(
                NotificationPreferences(
                    enabled = true,
                    time = LocalTime.of(7, 5),
                    weekdays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY),
                    weeklyGoal = 3,
                ),
            )

            assertEquals(NotificationPreferencesRequestDto(true, "07:05", listOf("MONDAY", "FRIDAY"), 3), service.lastUpdate)
        }

    private class FakeService(
        private val time: JsonElement,
    ) : NotificationApiService {
        var lastUpdate: NotificationPreferencesRequestDto? = null

        private fun preferences() = BaseResponse(200, 200, "ok", NotificationPreferencesDto(true, time, listOf("FRIDAY"), 3))

        override suspend fun getPreferences(): BaseResponse<NotificationPreferencesDto> = preferences()

        override suspend fun updatePreferences(request: NotificationPreferencesRequestDto): BaseResponse<NotificationPreferencesDto> {
            lastUpdate = request
            return preferences()
        }

        override suspend fun getInbox(page: Int): BaseResponse<NotificationInboxDto> = TODO("이 테스트 미사용")

        override suspend fun getTarget(notificationId: Long): BaseResponse<NotificationTargetDto> = TODO("이 테스트 미사용")

        override suspend fun markRead(notificationId: Long): BaseResponse<Unit> = TODO("이 테스트 미사용")

        override suspend fun reportEntry(): BaseResponse<NotificationEntryDto> = TODO("이 테스트 미사용")

        override suspend fun getDelegate(): BaseResponse<NotificationDelegateDto> = TODO("이 테스트 미사용")

        override suspend fun updateDelegate(request: NotificationDelegateRequestDto): BaseResponse<Unit> = TODO("이 테스트 미사용")

        override suspend fun deleteDelegate(): BaseResponse<Unit> = TODO("이 테스트 미사용")

        override suspend fun getAnniversaries(): BaseResponse<List<NotificationAnniversaryDto>> = TODO("이 테스트 미사용")

        override suspend fun createAnniversary(request: NotificationAnniversaryRequestDto): BaseResponse<NotificationAnniversaryDto> =
            TODO("이 테스트 미사용")

        override suspend fun deleteAnniversary(anniversaryId: Long): BaseResponse<Unit> = TODO("이 테스트 미사용")
    }
}
