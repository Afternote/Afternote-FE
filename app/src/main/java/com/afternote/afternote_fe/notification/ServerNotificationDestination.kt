package com.afternote.afternote_fe.notification

import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.ui.Route
import com.afternote.core.ui.SettingStart
import com.afternote.feature.mindrecord.presentation.navigation.MindRecordRoute
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute

/**
 * 알림이 열 화면. 탭 하나이거나, 탭 안쪽 화면이거나, 설정 안쪽 화면이다.
 *
 * [tab] 은 그 화면을 품은 최상위 목적지다. 안쪽 화면을 열 수 없을 때(이전 빌드가 게시한 알림의
 * 탭 extra 등) 이리로 떨어진다.
 */
internal sealed interface NotificationNavigation {
    val tab: NotificationDestination

    data class Tab(
        override val tab: NotificationDestination,
    ) : NotificationNavigation

    /** 마음의 기록 탭 위에 [route] 를 연다. */
    data class MindRecord(
        val route: MindRecordRoute,
    ) : NotificationNavigation {
        override val tab = NotificationDestination.MIND_RECORD
    }

    /** 타임레터 탭 위에 [routes] 를 차례로 쌓는다. 뒤로 가면 쌓은 역순으로 탭까지 돌아온다. */
    data class TimeLetter(
        val routes: List<TimeLetterRoute>,
    ) : NotificationNavigation {
        override val tab = NotificationDestination.TIME_LETTER
    }

    /** 설정을 [route] 의 시작 화면으로 연다. 바텀바 탭이 아니라 홈 위에 쌓인다. */
    data class Setting(
        val route: Route.Setting,
    ) : NotificationNavigation {
        override val tab = NotificationDestination.HOME
    }
}

/**
 * 서버 알림 data(BE `PushPayload`·`users/notifications/{id}/target`)를 앱이 열 화면으로 옮긴다 (#2321).
 *
 * 필수 ID 가 없거나 숫자가 아니면 그 목적지를 품은 탭으로 접는다 — 알림을 눌렀는데 아무 데도 안 가는
 * 편이 더 나쁘다. 앱에 화면이 없거나 앱 화면이 아닌 목적지(보관 확인·대리인 웹 확인·초대 랜딩·결제)는
 * 홈이다. 보관 확인은 앱을 연 것 자체가 확인이라 홈이 맞다 — 진입 보고가 절차를 취소한다.
 *
 * 일기·깊은 생각 상세는 아직 열지 않는다. 상세 화면이 일기를 «달» 로 찾는데 payload 에 그 달이 없어서,
 * 열면 작년 기록이 「없음」 으로 뜬다. 탭으로 보낸다.
 */
internal object ServerNotificationDestination {
    fun resolve(data: Map<String, String>): NotificationNavigation {
        val action = data[KEY_ACTION]
        return when (data[KEY_DESTINATION]?.trim()) {
            "RECORD_LIST", "WEEKLY_REPORT", "EASY_QUESTIONS" -> {
                NotificationNavigation.Tab(NotificationDestination.MIND_RECORD)
            }

            "RECORD_DETAIL" -> {
                val recordId = data.positiveId(KEY_RECORD_ID)
                if (recordId != null && data[KEY_RECORD_TYPE] == RECORD_TYPE_DAILY_QUESTION) {
                    NotificationNavigation.MindRecord(MindRecordRoute.RecordDetailRoute(recordId = recordId, isDiary = false))
                } else {
                    NotificationNavigation.Tab(NotificationDestination.MIND_RECORD)
                }
            }

            // 오늘 질문 작성. 이미 답한 질문이면 서버 목적지 확인이 기록 상세로 바꿔 준다.
            "DAILY_QUESTION" -> {
                NotificationNavigation.MindRecord(MindRecordRoute.DailyQuestionWriteRoute())
            }

            "TIME_LETTER_LIST" -> {
                NotificationNavigation.Tab(NotificationDestination.TIME_LETTER)
            }

            "TIME_LETTER_DETAIL" -> {
                data.positiveId(KEY_TIME_LETTER_ID)?.let { id ->
                    val detail = TimeLetterRoute.TimeLetterDetailRoute(id)
                    // 「지금이면 수정할 수 있어요」 — 상세 위에 수정 화면을 바로 연다. 상세에는 수정 버튼이 없다.
                    val routes = if (action == ACTION_EDIT) listOf(detail, TimeLetterRoute.TimeLetterWriteRoute(id)) else listOf(detail)
                    NotificationNavigation.TimeLetter(routes)
                } ?: NotificationNavigation.Tab(NotificationDestination.TIME_LETTER)
            }

            "TIME_LETTER_COMPOSE" -> {
                NotificationNavigation.TimeLetter(
                    listOf(TimeLetterRoute.TimeLetterWriteRoute(prefillReceiverId = data.positiveId(KEY_RECEIVER_ID))),
                )
            }

            // 발신자용 초대 상세 화면은 아직 없다. 초대 상태는 수신인 목록에서 본다.
            "RECEIVER_LIST", "INVITATION_DETAIL" -> {
                NotificationNavigation.Setting(Route.Setting(start = SettingStart.RECIPIENT_LIST))
            }

            "RECEIVER_DETAIL" -> {
                NotificationNavigation.Setting(
                    Route.Setting(start = SettingStart.RECIPIENT_EDIT, receiverId = data.positiveId(KEY_RECEIVER_ID)),
                )
            }

            "AGENT_SETTINGS" -> {
                NotificationNavigation.Setting(Route.Setting(start = SettingStart.DELEGATE))
            }

            else -> {
                NotificationNavigation.Tab(NotificationDestination.HOME)
            }
        }
    }

    private fun Map<String, String>.positiveId(key: String): Long? = this[key]?.trim()?.toLongOrNull()?.takeIf { it > 0L }

    private const val KEY_DESTINATION = "destination"
    private const val KEY_ACTION = "action"
    private const val KEY_RECORD_ID = "recordId"
    private const val KEY_RECORD_TYPE = "recordType"
    private const val KEY_TIME_LETTER_ID = "timeLetterId"
    private const val KEY_RECEIVER_ID = "receiverId"
    private const val RECORD_TYPE_DAILY_QUESTION = "DAILY_QUESTION"
    private const val ACTION_EDIT = "EDIT"
}
