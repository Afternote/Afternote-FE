package com.afternote.feature.mindrecord.presentation.screen.sender

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.User
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.model.WeeklyReportEmotion
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.reporting.RecordingErrorReporter
import com.afternote.feature.mindrecord.presentation.usecase.ObserveWeeklyReportUseCase
import com.afternote.feature.mindrecord.presentation.viewmodel.WeeklyReportViewModel
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 감정 카드 본문 문구가 **상태를 키워드보다 먼저 보는지** (#725 리뷰 지적).
 *
 * 부분 성공(일부 완료 + 일부 대기)에서는 완료분 키워드가 `emotions` 에 실려 내려온다 —
 * BE `buildTopEmotions` 에 완료 게이트가 없다. 키워드 유무를 먼저 보면 아직 분석 중인데도
 * 폴백 요약이 최종 요약처럼 확정되고, 대기 분기는 도달 불가가 된다.
 *
 * 문구를 만드는 함수를 직접 부르지 않고 **화면에 실제로 뜬 글**로 본다 — 분기가 맞아도
 * 그 값이 카드에 실리지 않으면 사용자는 다른 문장을 읽는다 (#1674).
 *
 * 뷰포트를 길게 잡는 이유는 감정 카드가 주간리포트 `LazyColumn` 의 아래쪽 항목이라서다 —
 * 기본 높이에서는 컴포즈되지도 않아 「그 문구가 화면에 없다」가 «스크롤 밖» 과 구분되지 않는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h2400dp-xhdpi")
class EmotionCardDescriptionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `키워드가 이미 나왔어도 대기 중이면 최종 요약으로 확정하지 않는다`() {
        // 대기 3건 중 1건만 성공 — 키워드는 이미 하나 내려와 있다.
        renderScreen(analysis = EmotionAnalysis(total = 3, succeeded = 1, pending = 2, failed = 0), hasKeyword = true)

        assertNotShown(SUMMARY_TEXT)
        composeRule.onNodeWithText(string(R.string.mindrecord_weekly_report_summary_pending)).assertExists()
    }

    @Test
    fun `분석이 끝나야 서버 요약을 쓴다`() {
        renderScreen(analysis = EmotionAnalysis(total = 1, succeeded = 1, pending = 0, failed = 0), hasKeyword = true)

        composeRule.onNodeWithText(SUMMARY_TEXT).assertExists()
    }

    @Test
    fun `상태를 모르면 키워드 0건이라고 확정하지 않는다`() {
        // emotionAnalysis 가 없으면 상태는 UNKNOWN 이다 — 0 건으로 접지 않는다.
        renderScreen(analysis = null, hasKeyword = false)

        // 「키워드가 나오지 않았어요」 는 0 건으로 확정하는 문구다 — UNKNOWN 에서는 쓰지 않는다.
        assertEquals(
            "상태를 모르는데 키워드 0건으로 확정했다",
            0,
            composeRule.onAllNodesWithText("나오지 않았", substring = true).fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText(string(R.string.mindrecord_emotion_card_unknown_description)).assertExists()
    }

    private fun string(resId: Int): String = composeRule.activity.getString(resId)

    private fun assertNotShown(text: String) =
        assertEquals(
            "화면에 뜨면 안 되는 문구가 떠 있다: $text",
            0,
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().size,
        )

    private fun renderScreen(
        analysis: EmotionAnalysis?,
        hasKeyword: Boolean,
    ) {
        val viewModel =
            WeeklyReportViewModel(
                observeWeeklyReport =
                    ObserveWeeklyReportUseCase(
                        repository = EmotionCardWeeklyReportRepository(report(analysis, hasKeyword)),
                        userRepository =
                            FakeUserRepository.strict().apply {
                                onReceiverListFlow = { flowOf(emptyList()) }
                                onGetMyProfile = {
                                    User(name = "아담", email = "a@b.c", phone = null, profileImageUrl = null)
                                }
                            },
                    ),
                changeTracker = MindRecordChangeTracker(),
                errorReporter = RecordingErrorReporter(),
            )
        composeRule.setContent { AfternoteTheme { WeeklyReportScreen(viewModel = viewModel) } }
        composeRule.waitForIdle()
    }

    private fun report(
        analysis: EmotionAnalysis?,
        hasKeyword: Boolean,
    ) = WeeklyReport(
        dailyQuestionAmount = 0,
        diaryAmount = 1,
        summaryText = SUMMARY_TEXT,
        week = emptyList(),
        dailyQuestions = emptyList(),
        emotions = if (hasKeyword) listOf(WeeklyReportEmotion(keyword = "가족", percentage = 60)) else emptyList(),
        emotionAnalysis = analysis,
    )

    private companion object {
        const val SUMMARY_TEXT = "이번 주 기록을 바탕으로 인사이트를 준비 중이에요."
    }
}

private class EmotionCardWeeklyReportRepository(
    private val report: WeeklyReport,
) : WeeklyReportRepository {
    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> = Result.success(report)
}
