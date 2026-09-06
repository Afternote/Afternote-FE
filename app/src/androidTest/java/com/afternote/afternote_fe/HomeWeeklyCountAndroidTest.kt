package com.afternote.afternote_fe

import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.HiltTestActivity
import com.afternote.afternote_fe.test.appTestUserRepository
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserProfileCacheRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.home.presentation.HomeTabActions
import com.afternote.feature.home.presentation.HomeTabScreen
import com.afternote.feature.home.presentation.HomeTabUiState
import com.afternote.feature.home.presentation.HomeTabViewModel
import com.afternote.feature.home.presentation.usecase.GetHomeSummaryUseCase
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.usecase.GetWeeklyRecordCountUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.afternote.feature.home.presentation.R as HomeR

/**
 * 실제 홈 경로가 주간 기록 수를 그리드까지 **전달하는지** (#562).
 *
 * 원래 결함은 그리드가 값을 못 그린 것이 아니라 **호출부가 값을 넘기지 않은 것**이었다.
 * 컴포넌트에 숫자를 직접 넣는 테스트는 그 자리를 지나지 않으므로, ViewModel 이 만든 상태를
 * 화면에 물려 숫자가 실제로 보이는 데까지를 여기서 고정한다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeWeeklyCountAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // 홈은 MEMORIES 섹션에서 hiltViewModel() 을 부른다. 좁은 화면(Pixel 2 API 30)에서 주간
    // 카드까지 스크롤하면 그 항목도 composition 되므로 Hilt 가 붙은 Activity 여야 한다 —
    // 맨 ComponentActivity 로는 "does not implement GeneratedComponent" 로 죽는다.
    // MainActivity 는 이미 콘텐츠를 세팅해 setContent 가 거부되므로 빈 테스트 Activity 를 쓴다.
    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    @Before
    fun inject() {
        hiltRule.inject()
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
        awaitSuccess(viewModel)
        composeRule.scrollToWeeklyCard()
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

        awaitSuccess(viewModel)
        composeRule.scrollToWeeklyCard()
        composeRule.onNodeWithText(UNKNOWN_COUNT).assertIsDisplayed()
    }

    /**
     * 상태가 Success 가 될 때까지 기다린다. 화면 단언보다 먼저 두는 이유는, 조회가 끝나기 전에
     * 스크롤하면 주간 카드가 아직 목록에 없어 스크롤 자체가 실패하기 때문이다.
     */
    @Test
    fun homeScreen_timeLetterCtaClickRoutesToTimeLetter() {
        // 액션을 직접 호출하는 테스트는 화면 CTA 가 다른 액션에 연결돼도 통과한다.
        // 실제로 스크롤·클릭해서 목적지까지 본다 (#700 리뷰).
        val viewModel = homeViewModel(dailyQuestionAmount = 1, diaryAmount = 1)
        var routedToTimeLetter = false

        composeRule.setContent {
            AfternoteTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HomeTabScreen(
                    uiState = uiState,
                    actions = RecordingHomeTabActions { routedToTimeLetter = true },
                    todayDateText = "2026.04.10",
                )
            }
        }

        awaitSuccess(viewModel)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cta = context.getString(HomeR.string.home_tab_timeletter_next_step_cta)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(cta))
        composeRule.waitForIdle()
        composeRule.onNodeWithText(cta).performClick()
        composeRule.waitForIdle()

        assertTrue("타임레터 CTA 가 타임레터 액션으로 연결돼야 한다", routedToTimeLetter)
    }

    private fun awaitSuccess(viewModel: HomeTabViewModel) {
        composeRule.waitUntil(timeoutMillis = TIMEOUT) {
            viewModel.uiState.value is HomeTabUiState.Success
        }
    }

    /**
     * 주간 카드까지 스크롤한다.
     *
     * `WeeklySummaryGrid` 는 `LazyColumn` 의 뒤쪽 item 이라 좁은 화면(Pixel 2 API 30)에서는
     * 아직 composition 되지 않는다. 노드가 «생기기만» 기다리면 스스로 노출되지 않아 timeout
     * 이다 — 내 에뮬레이터(화면이 더 큼)에서만 통과했던 이유다 (#562 리뷰).
     */
    private fun ComposeContentTestRule.scrollToWeeklyCard() {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(WEEKLY_CARD_LABEL))
        waitForIdle()
    }

    private fun homeViewModel(
        dailyQuestionAmount: Int = 0,
        diaryAmount: Int = 0,
        weeklyFailure: Throwable? = null,
    ): HomeTabViewModel {
        // 같은 페이크가 두 좁은 계약을 다 구현한다 — UserRepository 가 둘을 상속한다 (#1742).
        val userRepository = appTestUserRepository()
        return HomeTabViewModel(
            getHomeSummary =
                GetHomeSummaryUseCase(
                    myProfileRepository = userRepository,
                    userReceiverRepository = userRepository,
                    dailyQuestionRepository = FakeDailyQuestionRepository(),
                    getWeeklyRecordCount =
                        GetWeeklyRecordCountUseCase(
                            FakeWeeklyReportRepository(dailyQuestionAmount, diaryAmount, weeklyFailure),
                        ),
                ),
            userProfileCacheRepository = FakeUserProfileCacheRepository(),
            errorReporter = SilentErrorReporter,
        )
    }

    private companion object {
        const val TIMEOUT = 5_000L
        const val EXPECTED_COUNT = "7"
        const val WEEKLY_CARD_LABEL = "THIS WEEK"
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

/** 타임레터 CTA 만 관찰하는 no-op 액션. 모듈의 Noop 이 private 이라 여기서 만든다. */
private class RecordingHomeTabActions(
    private val onTimeLetter: () -> Unit,
) : HomeTabActions {
    override fun onRecipientChipClick() = Unit

    override fun onAnswerClick() = Unit

    override fun onNextStepClick() = Unit

    override fun onTimeLetterNextStepClick() = onTimeLetter()

    override fun onWeeklyImageClick() = Unit

    override fun onWeeklyCountClick() = Unit

    override fun onMemoriesSectionClick() = Unit

    override fun onMemoriesRecordDetailClick(recordId: Long) = Unit

    override fun onSettingClick() = Unit

    override fun onRetryLoad() = Unit
}
