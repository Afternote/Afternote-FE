package com.afternote.afternote_fe.notification

import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.ui.Route
import com.afternote.core.ui.SettingStart
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 서버 목적지 계약(BE `PushPayload` whitelist)을 앱 화면으로 옮기는 규칙의 회귀 기준 (#2321).
 *
 * 서버 목적지 하나하나가 어디로 가는지와, 필수 ID 가 깨졌을 때 그 목적지를 품은 탭으로 접히는지를 본다.
 */
class ServerNotificationDestinationTest {
    @Test
    fun `서버 whitelist 의 모든 목적지가 열 화면으로 옮겨진다`() {
        val expected =
            mapOf(
                mapOf("destination" to "HOME") to tab(NotificationDestination.HOME),
                mapOf("destination" to "RECORD_LIST") to tab(NotificationDestination.MIND_RECORD),
                mapOf("destination" to "WEEKLY_REPORT") to tab(NotificationDestination.MIND_RECORD),
                mapOf("destination" to "EASY_QUESTIONS") to tab(NotificationDestination.MIND_RECORD),
                mapOf("destination" to "RECORD_DETAIL", "recordType" to "DAILY_QUESTION", "recordId" to "3") to
                    NotificationNavigation.MindRecord(MindRecordRoute.RecordDetailRoute(recordId = 3L, isDiary = false)),
                mapOf("destination" to "DAILY_QUESTION", "questionId" to "8") to
                    NotificationNavigation.MindRecord(MindRecordRoute.DailyQuestionWriteRoute()),
                mapOf("destination" to "TIME_LETTER_LIST") to tab(NotificationDestination.TIME_LETTER),
                mapOf("destination" to "TIME_LETTER_DETAIL", "timeLetterId" to "12") to
                    NotificationNavigation.TimeLetter(listOf(TimeLetterRoute.TimeLetterDetailRoute(12L))),
                mapOf("destination" to "TIME_LETTER_COMPOSE", "receiverId" to "4") to
                    NotificationNavigation.TimeLetter(listOf(TimeLetterRoute.TimeLetterWriteRoute(prefillReceiverId = 4L))),
                mapOf("destination" to "RECEIVER_LIST") to setting(Route.Setting(start = SettingStart.RECIPIENT_LIST)),
                mapOf("destination" to "INVITATION_DETAIL", "invitationId" to "6") to
                    setting(Route.Setting(start = SettingStart.RECIPIENT_LIST)),
                mapOf("destination" to "RECEIVER_DETAIL", "receiverId" to "4") to
                    setting(Route.Setting(start = SettingStart.RECIPIENT_EDIT, receiverId = 4L)),
                mapOf("destination" to "AGENT_SETTINGS") to setting(Route.Setting(start = SettingStart.DELEGATE)),
                mapOf("destination" to "RETENTION_CONFIRMATION") to tab(NotificationDestination.HOME),
                mapOf("destination" to "AGENT_CONFIRMATION", "linkId" to "x") to tab(NotificationDestination.HOME),
                mapOf("destination" to "INVITATION", "linkId" to "x") to tab(NotificationDestination.HOME),
                mapOf("destination" to "RECEIVER_HOME") to tab(NotificationDestination.HOME),
                mapOf("destination" to "PLAN") to tab(NotificationDestination.HOME),
                mapOf("destination" to "SUBSCRIPTION_MANAGEMENT") to tab(NotificationDestination.HOME),
            )

        expected.forEach { (data, navigation) ->
            assertEquals("$data", navigation, ServerNotificationDestination.resolve(data))
        }
    }

    @Test
    fun `수정 강조 알림은 편지 상세 위에 수정 화면을 쌓는다`() {
        assertEquals(
            NotificationNavigation.TimeLetter(
                listOf(TimeLetterRoute.TimeLetterDetailRoute(12L), TimeLetterRoute.TimeLetterWriteRoute(12L)),
            ),
            ServerNotificationDestination.resolve(mapOf("destination" to "TIME_LETTER_DETAIL", "timeLetterId" to "12", "action" to "EDIT")),
        )
    }

    @Test
    fun `필수 ID 가 없거나 숫자가 아니면 그 목적지를 품은 탭으로 접는다`() {
        assertEquals(
            tab(NotificationDestination.TIME_LETTER),
            ServerNotificationDestination.resolve(mapOf("destination" to "TIME_LETTER_DETAIL", "timeLetterId" to "abc")),
        )
        assertEquals(
            tab(NotificationDestination.MIND_RECORD),
            ServerNotificationDestination.resolve(mapOf("destination" to "RECORD_DETAIL", "recordType" to "DAILY_QUESTION")),
        )
        assertEquals(
            setting(Route.Setting(start = SettingStart.RECIPIENT_EDIT, receiverId = null)),
            ServerNotificationDestination.resolve(mapOf("destination" to "RECEIVER_DETAIL", "receiverId" to "-1")),
        )
    }

    @Test
    fun `일기 상세는 찾을 달을 몰라 기록 탭으로 연다`() {
        listOf("DIARY", "DEEP_THOUGHT").forEach { type ->
            assertEquals(
                tab(NotificationDestination.MIND_RECORD),
                ServerNotificationDestination.resolve(mapOf("destination" to "RECORD_DETAIL", "recordType" to type, "recordId" to "3")),
            )
        }
    }

    @Test
    fun `목적지가 없거나 모르는 값이면 홈이다`() {
        listOf(emptyMap(), mapOf("destination" to "UNKNOWN"), mapOf("destination" to " ")).forEach { data ->
            assertEquals(tab(NotificationDestination.HOME), ServerNotificationDestination.resolve(data))
        }
    }

    private fun tab(destination: NotificationDestination) = NotificationNavigation.Tab(destination)

    private fun setting(route: Route.Setting) = NotificationNavigation.Setting(route)
}
