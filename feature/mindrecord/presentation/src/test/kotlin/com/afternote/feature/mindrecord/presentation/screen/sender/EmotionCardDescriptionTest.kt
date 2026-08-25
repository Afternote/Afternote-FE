package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
import com.afternote.feature.mindrecord.presentation.model.EmotionKeyword
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * 감정 카드 본문 문구가 **상태를 키워드보다 먼저 보는지** (#725 리뷰 지적).
 *
 * 부분 성공(일부 완료 + 일부 대기)에서는 완료분 키워드가 `emotions` 에 실려 내려온다 —
 * BE `buildTopEmotions` 에 완료 게이트가 없다. 키워드 유무를 먼저 보면 아직 분석 중인데도
 * 폴백 요약이 최종 요약처럼 확정되고, 대기 분기는 도달 불가가 된다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EmotionCardDescriptionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun state(
        status: EmotionAnalysisStatus,
        keywords: List<EmotionKeyword> = emptyList(),
    ) = WeeklyReportUiState.Success(
        selectedMonday = LocalDate.of(2026, 8, 24),
        weekOptions = emptyList(),
        dateRange = "",
        userName = "아담",
        recordedDays = 1,
        counts = emptyList(),
        emotionAnalysisStatus = status,
        weekDays = emptyList(),
        emotionKeywords = keywords,
        summaryText = "이번 주 기록을 바탕으로 인사이트를 준비 중이에요.",
        dailyQuestions = emptyList(),
    )

    private fun describe(state: WeeklyReportUiState.Success): String {
        lateinit var text: String
        composeRule.setContent {
            AfternoteTheme { text = emotionCardDescription(state) }
        }
        composeRule.waitForIdle()
        return text
    }

    @Test
    fun `키워드가 이미 나왔어도 대기 중이면 최종 요약으로 확정하지 않는다`() {
        val described =
            describe(
                state(
                    status = EmotionAnalysisStatus.PENDING,
                    keywords = listOf(EmotionKeyword(keyword = "가족", count = 60)),
                ),
            )

        assert(described != "이번 주 기록을 바탕으로 인사이트를 준비 중이에요.") {
            "부분 성공에서 폴백 요약이 최종 요약처럼 확정됐다: $described"
        }
    }

    @Test
    fun `분석이 끝나야 서버 요약을 쓴다`() {
        val described =
            describe(
                state(
                    status = EmotionAnalysisStatus.COMPLETED,
                    keywords = listOf(EmotionKeyword(keyword = "가족", count = 60)),
                ),
            )

        assert(described == "이번 주 기록을 바탕으로 인사이트를 준비 중이에요.") { described }
    }

    @Test
    fun `상태를 모르면 키워드 0건이라고 확정하지 않는다`() {
        val described = describe(state(status = EmotionAnalysisStatus.UNKNOWN))

        assert(!described.contains("나오지 않았")) { described }
    }
}
