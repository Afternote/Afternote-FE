package com.afternote.feature.mindrecord.presentation.screen.receiver

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import com.afternote.feature.mindrecord.presentation.component.ReceiverRecordDetailSheet
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordFilter
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 수신자 기록 본문 열람 가드 (#618).
 *
 * 카드가 clickable 인데 콜백이 빈 람다라 탭해도 아무 일이 없었고, 목록에서 제목만 보이고
 * `content`·`todayMood` 에 도달할 경로가 없었다. 서버는 목록 응답에 본문을 함께 주므로
 * 화면이 들고 있는 항목을 펼치면 된다 — 추가 조회가 없다는 점도 함께 고정한다.
 *
 * 「탭한 기록을 어떻게 찾는가」는 화면 내부의 도우미가 아니라 **화면을 실제로 눌러** 본다.
 * 도우미를 직접 부르면 카드 탭과 시트 표시 사이의 배선이 빠져도 초록이다 — 이 이슈가 고친
 * 결함이 정확히 그 배선이었다 (#1674).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceiverRecordDetailTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val diary =
        MindRecordSummary(
            id = 11L,
            type = MindRecordType.DIARY,
            title = "오늘의 산책",
            content = "<p>강변을 한 시간 걸었다.</p>",
            recordDate = "2026-07-29",
            isDraft = false,
            createdAt = "2026.07.29 수",
            todayMood = TodayMood.HAPPY,
        )

    private val answer =
        MindRecordSummary(
            id = 22L,
            type = MindRecordType.DAILY_QUESTION,
            title = "가장 기억에 남는 여행지는?",
            content = "<p>제주.</p>",
            recordDate = "2026-07-28",
            isDraft = false,
            createdAt = "2026.07.28 화",
        )

    @Test
    fun `탭한 데일리질문의 본문이 열린다`() {
        renderScreen()

        composeRule.onNodeWithText(answer.title).performClick()

        composeRule.onNodeWithText("제주.").assertIsDisplayed()
    }

    @Test
    fun `일기 탭의 카드도 같은 경로로 열린다`() {
        renderScreen()

        composeRule.onNodeWithText(DIARY_TAB).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(diary.title).performClick()

        composeRule.onNodeWithText("강변을 한 시간 걸었다.").assertIsDisplayed()
    }

    @Test
    fun `아무것도 안 눌렀으면 본문이 열리지 않는다`() {
        renderScreen()

        assertEquals(
            "탭하지 않았는데 본문이 떠 있다",
            0,
            composeRule.onAllNodesWithText("제주.").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun `필터로 목록에서 빠지면 열려 있던 본문이 닫힌다`() {
        // 시트가 열린 채 필터가 바뀌면 자연히 닫힌다 — 없는 기록을 계속 붙들지 않는다.
        val viewModel = renderScreen()
        composeRule.onNodeWithText(answer.title).performClick()
        composeRule.onNodeWithText("제주.").assertIsDisplayed()

        composeRule.runOnIdle {
            viewModel.applyFilter(ReceiverMindRecordFilter(fromDate = "2026-08-01", toDate = "2026-08-31"))
        }
        composeRule.waitForIdle()

        assertEquals(
            "목록에서 빠진 기록의 본문이 남아 있다",
            0,
            composeRule.onAllNodesWithText("제주.").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun `본문 시트가 제목과 본문과 기분을 보여준다`() {
        // 목록에서 제목만 보이던 상태에서 content·todayMood 에 도달하는 것이 이 이슈의 목적이다.
        composeRule.setContent {
            AfternoteTheme {
                ReceiverRecordDetailSheet(record = diary, onDismiss = {})
            }
        }

        composeRule.onNodeWithText("오늘의 산책").assertIsDisplayed()
        // HTML 조각이라 태그를 벗겨 읽을 수 있는 텍스트로 보여준다.
        composeRule.onNodeWithText("강변을 한 시간 걸었다.").assertIsDisplayed()
        composeRule.onNodeWithText("2026.07.29 수").assertIsDisplayed()
        composeRule.onNodeWithText("😊").assertIsDisplayed()
    }

    @Test
    fun `기분이 없는 데일리질문은 기분 자리를 만들지 않는다`() {
        composeRule.setContent {
            AfternoteTheme {
                ReceiverRecordDetailSheet(record = answer, onDismiss = {})
            }
        }

        composeRule.onNodeWithText("제주.").assertIsDisplayed()
        assertEquals(
            "기분이 없는 기록에는 이모지 자리가 없다",
            0,
            composeRule.onAllNodesWithText("😊").fetchSemanticsNodes().size,
        )
    }

    private fun renderScreen(): ReceiverMindRecordViewModel {
        val viewModel =
            ReceiverMindRecordViewModel(
                repository = FakeReceiverRepository(dailyQuestions = listOf(answer), diaries = listOf(diary)),
                errorReporter = RecordingErrorReporter(),
            )
        composeRule.setContent {
            AfternoteTheme {
                ReceiverMindRecordScreen(viewModel = viewModel, onBackClick = {})
            }
        }
        composeRule.waitForIdle()
        return viewModel
    }

    private companion object {
        const val DIARY_TAB = "일기"
    }
}

private class FakeReceiverRepository(
    private val dailyQuestions: List<MindRecordSummary>,
    private val diaries: List<MindRecordSummary>,
) : MindRecordReceiverRepository {
    override suspend fun getAll(): Result<ReceiverMindRecords> =
        Result.success(ReceiverMindRecords(dailyQuestions = dailyQuestions, diaries = diaries))
}
