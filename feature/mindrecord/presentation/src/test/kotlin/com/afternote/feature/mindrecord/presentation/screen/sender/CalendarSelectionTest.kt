package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 캘린더 날짜 셀 상호작용 가드 (#724).
 *
 * 공용 `DailyCalendar` 에 `selectedDay`·`onDayClick` 이 아예 없어, 날짜를 눌러도 선택
 * 표시도 기록 필터도 일어나지 않았다. 월 이동만 되고 날짜는 죽어 있던 상태다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CalendarSelectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `날짜 셀을 누르면 그 일자가 콜백으로 온다`() {
        var clicked: Int? = null
        composeRule.setContent {
            AfternoteTheme {
                DailyCalendar(
                    year = 2026,
                    month = 8,
                    type = MindRecordCategoryUi.Diary,
                    onPrevMonth = {},
                    onNextMonth = {},
                    answeredDays = setOf(12),
                    onDayClick = { clicked = it },
                )
            }
        }

        composeRule.onAllNodesWithText("12")[0].assertHasClickAction()
        composeRule.onAllNodesWithText("12")[0].performClick()

        assertEquals(12, clicked)
    }

    @Test
    fun `콜백을 주지 않으면 셀은 클릭 대상이 아니다`() {
        // 수신자 화면 등 선택이 없는 곳에서 헛클릭이 생기지 않게 한다.
        composeRule.setContent {
            AfternoteTheme {
                DailyCalendar(
                    year = 2026,
                    month = 8,
                    type = MindRecordCategoryUi.Diary,
                    onPrevMonth = {},
                    onNextMonth = {},
                )
            }
        }

        composeRule.onAllNodesWithText("12")[0].assertHasNoClickAction()
    }

    @Test
    fun `월 이동 수단은 기록이 없는 달에도 남는다`() {
        // 종전에는 빈 상태가 캘린더를 통째로 대체해 이전·다음 월 버튼까지 사라졌다.
        composeRule.setContent {
            AfternoteTheme {
                DailyCalendar(
                    year = 2026,
                    month = 8,
                    type = MindRecordCategoryUi.Diary,
                    onPrevMonth = {},
                    onNextMonth = {},
                    answeredDays = emptySet(),
                )
            }
        }

        composeRule.onNodeWithText("2026년 8월").assertIsDisplayed()
        // 일기 탭이므로 「기록」이다 — 종전에는 두 탭이 문구를 공유해 여기서도 「답변」이었고,
        // 이 단언이 그 오문을 기대값으로 굳히고 있었다 (#1712).
        composeRule.onNodeWithText("0개의 기록 완료").assertIsDisplayed()
    }
}
