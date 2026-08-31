package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth

/**
 * 목록이 **보고 있는 달**을 상세로 넘기는지 (#759 리뷰).
 *
 * 종전에는 내비게이션이 `YearMonth.now()` 를 넣어, 지난달 기록을 탭하면 상세가 이번 달
 * 목록에서 그 기록을 찾다 실패했다 — 이번 달 기록만 열렸다. 목록은 달을 바꿀 수 있는
 * 화면이라 흔히 밟히는 경로다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordListMonthTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `카드를 누르면 보고 있는 달이 함께 전달된다`() {
        val lastMonth = YearMonth.now().minusMonths(1)
        var clicked: Pair<Long, YearMonth>? = null

        composeRule.setContent {
            AfternoteTheme {
                DiaryListContent(
                    isListView = true,
                    yearMonth = lastMonth,
                    onYearMonthChanged = {},
                    diaries =
                        listOf(
                            DailyDiary(
                                id = 11L,
                                title = "지난달 기록",
                                date = lastMonth.atDay(3),
                                content = "본문",
                            ),
                        ),
                    onItemClick = { id, month -> clicked = id to month },
                )
            }
        }

        composeRule.onAllNodesWithText("지난달 기록")[0].performClick()

        assertEquals(11L to lastMonth, clicked)
    }
}
