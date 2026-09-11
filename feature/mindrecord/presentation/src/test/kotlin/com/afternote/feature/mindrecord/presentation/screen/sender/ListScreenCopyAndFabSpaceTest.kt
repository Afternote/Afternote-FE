package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.FAB.AfternoteFabContentBottomPadding
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.component.DailyCalendar
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * 목록 화면의 **문구**와 **FAB 자리** 가드 (#1712 · #1713).
 *
 * 둘 다 #269 QA 회귀를 실기에서 돌다가 나온 것이다. 컴파일도 기존 테스트도 잡지 못했다 —
 * 문구는 타입이 같고, 가림은 레이아웃이라 단언할 대상이 없었다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xhdpi")
class ListScreenCopyAndFabSpaceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * 같은 캘린더를 두 탭이 쓰는데 카운트 문구가 하나뿐이라, 일기 탭에서도 「N개의 **답변**
     * 완료」로 읽혔다. 두 탭을 **각각** 렌더해 서로의 문구를 쓰지 않는 것까지 본다 —
     * 한쪽만 보면 둘을 도로 합쳐도 통과한다.
     */
    @Test
    fun `캘린더 카운트 문구가 탭마다 다르다`() {
        renderCalendar(MindRecordCategoryUi.DailyQuestion)
        composeRule.onNodeWithText("1개의 답변 완료").assertIsDisplayed()
        composeRule.onNodeWithText("1개의 기록 완료").assertDoesNotExist()

        composeRule.runOnIdle { category.value = MindRecordCategoryUi.Diary }
        composeRule.onNodeWithText("1개의 기록 완료").assertIsDisplayed()
        composeRule.onNodeWithText("1개의 답변 완료").assertDoesNotExist()
    }

    /** 일기 목록인데 데일리질문 헤더(`DAILY ANSWER`)가 붙어 있었다. */
    @Test
    fun `일기 목록 섹션 헤더가 일기 문구다`() {
        composeRule.setContent {
            AfternoteTheme {
                DiaryListContent(
                    isListView = true,
                    yearMonth = YearMonth.of(2026, 9),
                    diaries = listOf(diary()),
                    onItemClick = { _, _ -> },
                    onEdit = { _, _ -> },
                    onDelete = {},
                    onYearMonthChanged = {},
                )
            }
        }

        composeRule.onNodeWithText("DAILY ANSWER").assertDoesNotExist()
        composeRule.onNodeWithText("DAILY DIARY").assertIsDisplayed()
    }

    /**
     * FAB 은 `Scaffold` 가 콘텐츠 위에 띄우고 자리를 예약해 주지 않는다. 목록이 스스로
     * 비우지 않으면 마지막 항목이 가려지는데, **스크롤이 없을 만큼 항목이 적으면 볼 방법이
     * 없다.** 여백이 FAB 을 실제로 덮고도 남는지 값으로 못 박는다.
     */
    @Test
    fun `목록 하단 여백이 FAB 을 덮고도 남는다`() {
        assertTrue(
            "FAB(56dp)보다 여백이 작으면 마지막 항목이 가려진다: $AfternoteFabContentBottomPadding",
            AfternoteFabContentBottomPadding >= 56.dp,
        )
        // 여백만 크면 되는 게 아니라 «FAB 지름 + 위아래 화면 여백» 이어야 바로 위에 붙지 않는다.
        assertTrue(
            "FAB 바로 위에 항목이 붙는다: $AfternoteFabContentBottomPadding",
            AfternoteFabContentBottomPadding >= 56.dp + 16.dp * 2,
        )
    }

    private val category = mutableStateOf<MindRecordCategoryUi>(MindRecordCategoryUi.DailyQuestion)

    private fun renderCalendar(initial: MindRecordCategoryUi) {
        category.value = initial
        composeRule.setContent {
            AfternoteTheme {
                CalendarUnderTest(category.value)
            }
        }
    }

    @Composable
    private fun CalendarUnderTest(type: MindRecordCategoryUi) {
        DailyCalendar(
            year = 2026,
            month = 9,
            type = type,
            onPrevMonth = {},
            onNextMonth = {},
            answeredDays = setOf(1),
            emotionByDay = emptyMap(),
            selectedDay = null,
            onDayClick = {},
        )
    }

    private fun diary() =
        DailyDiary(
            id = 1L,
            title = "일기",
            date = LocalDate.of(2026, 9, 1),
            content = "본문",
        )
}
