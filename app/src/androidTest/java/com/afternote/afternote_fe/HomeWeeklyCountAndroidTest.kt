package com.afternote.afternote_fe

import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserProfileRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.HomeTabScreen
import com.afternote.feature.home.presentation.HomeTabViewModel
import com.afternote.feature.home.presentation.usecase.GetHomeSummaryUseCase
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.usecase.GetWeeklyRecordCountUseCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 실제 홈 경로가 주간 기록 수를 그리드까지 **전달하는지** (#562).
 *
 * 원래 결함은 그리드가 값을 못 그린 것이 아니라 **호출부가 값을 넘기지 않은 것**이었다.
 * 컴포넌트에 숫자를 직접 넣는 테스트는 그 자리를 지나지 않으므로, ViewModel 이 만든 상태를
 * 화면에 물려 숫자가 실제로 보이는 데까지를 여기서 고정한다.
 */
@RunWith(AndroidJUnit4::class)
class HomeWeeklyCountAndroidTest {
    @get:Rule(order = 0)
    val composeRule = createComposeRule()

    @get:Rule(order = 1)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Test
    fun homeScreen_showsWeeklyRecordCountFromViewModel() {
        val viewModel = homeViewModel(dailyQuestionAmount = 3, diaryAmount = 4)

        composeRule.setContent {
            AfternoteTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeTabScreen(uiState = uiState, todayDateText = "2026.04.10")
            }
        }

        // 3 + 4 = 7. 이 숫자가 보이려면 UseCase → UiState → HomeTabScreen → WeeklySummaryGrid
        // 배선이 전부 살아 있어야 한다.
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            composeRule.onAllNodesWithText(EXPECTED_COUNT).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(EXPECTED_COUNT).assertIsDisplayed()
    }

    @Test
    fun homeScreen_showsPlaceholderWhenWeeklyCountUnavailable() {
        // 조회 실패를 0 으로 접으면 «기록이 없음» 을 확정한다 — 미상은 대시로 남아야 한다 (#562).
        val viewModel = homeViewModel(weeklyFailure = IllegalStateException("주간 조회 실패"))

        composeRule.setContent {
            AfternoteTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeTabScreen(uiState = uiState, todayDateText = "2026.04.10")
            }
        }

        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            composeRule.onAllNodesWithText(UNKNOWN_COUNT).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(UNKNOWN_COUNT).assertIsDisplayed()
    }

    private fun homeViewModel(
        dailyQuestionAmount: Int = 0,
        diaryAmount: Int = 0,
        weeklyFailure: Throwable? = null,
    ): HomeTabViewModel =
        HomeTabViewModel(
            getHomeSummary =
                GetHomeSummaryUseCase(
                    userRepository = appTestUserRepository(),
                    dailyQuestionRepository = FakeDailyQuestionRepository(),
                    getWeeklyRecordCount =
                        GetWeeklyRecordCountUseCase(
                            FakeWeeklyReportRepository(dailyQuestionAmount, diaryAmount, weeklyFailure),
                        ),
                ),
            userProfileRepository = FakeUserProfileRepository(),
            errorReporter = SilentErrorReporter,
        )

    private companion object {
        const val TIMEOUT = 5_000L
        const val EXPECTED_COUNT = "7"
        const val UNKNOWN_COUNT = "–"
    }
}

/** 이 테스트는 보고 여부를 보지 않는다 — 조회 실패 경로가 리포터를 부르므로 자리만 채운다. */
private object SilentErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = Unit
}

private class FakeWeeklyReportRepository(
    private val dailyQuestionAmount: Int,
    private val diaryAmount: Int,
    private val failure: Throwable?,
) : WeeklyReportRepository {
    override suspend fun getWeeklyReport(date: String): Result<WeeklyReport> {
        failure?.let { return Result.failure(it) }
        return Result.success(
            WeeklyReport(
                dailyQuestionAmount = dailyQuestionAmount,
                diaryAmount = diaryAmount,
                summaryText = "",
                week = emptyList(),
                dailyQuestions = emptyList(),
                emotions = emptyList(),
                emotionAnalysis = null,
            ),
        )
    }
}
