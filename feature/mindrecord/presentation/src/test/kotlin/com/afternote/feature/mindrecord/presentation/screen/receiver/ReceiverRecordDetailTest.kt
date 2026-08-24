package com.afternote.feature.mindrecord.presentation.screen.receiver

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.component.ReceiverRecordDetailSheet
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordFilter
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun success() =
        ReceiverMindRecordUiState.Success(
            dailyQuestions = listOf(answer),
            diaries = listOf(diary),
            filter = ReceiverMindRecordFilter(),
        )

    @Test
    fun `탭한 일기를 두 탭 목록에서 찾아낸다`() {
        assertEquals(diary, findOpenedRecord(success(), 11L))
    }

    @Test
    fun `탭한 데일리질문도 같은 경로로 찾는다`() {
        assertEquals(answer, findOpenedRecord(success(), 22L))
    }

    @Test
    fun `아무것도 안 눌렀으면 열지 않는다`() {
        assertNull(findOpenedRecord(success(), null))
    }

    @Test
    fun `필터로 목록에서 빠진 항목은 열리지 않는다`() {
        // 시트가 열린 채 필터가 바뀌면 자연히 닫힌다 — 없는 기록을 계속 붙들지 않는다.
        assertNull(findOpenedRecord(success(), 999L))
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
}
