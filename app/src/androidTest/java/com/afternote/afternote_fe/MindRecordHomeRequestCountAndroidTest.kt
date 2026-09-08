package com.afternote.afternote_fe

import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.AppState
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysis
import com.afternote.feature.mindrecord.domain.model.WeeklyReport
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDailyQuestionRepository
import com.afternote.feature.mindrecord.domain.testing.FakeDiaryRepository
import com.afternote.feature.mindrecord.domain.testing.FakeWeeklyReportRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * 마음의 기록 첫 진입 요청 수를 **실제 화면 배선 위에서** 센다 (#736).
 *
 * `MindRecordRequestCountTest`(JVM) 는 ViewModel 을 직접 만들어 `refreshOnReturn()` 을 손으로
 * 부른다 — 그래서 [HomeScreen] 의 `HorizontalPager`, 탭 클릭, 화면별 `hiltViewModel()`,
 * `LifecycleEventEffect` 를 하나도 지나지 않는다. 그 배선에서 VM 이 조기 생성되거나 0→2 이동 중
 * 일기 탭이 합성돼도 JVM 테스트는 계속 통과한다 (#736 리뷰).
 *
 * 그래서 여기서는 **실제 `AppNavigation` + Hilt 가 주입한 counting fake** 로 요청을 센다.
 * 화면을 바꾸는 것이 아니라 «몇 번 불렀나» 를 보는 테스트라, 단언은 전부 저장소 호출 기록이다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class MindRecordHomeRequestCountAndroidTest {
    @Inject
    lateinit var dailyQuestionRepository: DailyQuestionRepository

    @Inject
    lateinit var diaryRepository: DiaryRepository

    @Inject
    lateinit var weeklyReportRepository: WeeklyReportRepository

    private val dailyQuestion get() = dailyQuestionRepository as FakeDailyQuestionRepository
    private val diary get() = diaryRepository as FakeDiaryRepository
    private val weekly get() = weeklyReportRepository as FakeWeeklyReportRepository

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        hiltRule.inject()
        // 큐가 비면 fake 가 터진다 — 주간 탭을 여러 번 열어도 남도록 넉넉히 채운다.
        repeat(WEEKLY_RESPONSE_SLOTS) { weekly.results.addLast(Result.success(emptyWeeklyReport())) }
        composeRule.activityRule.scenario.onActivity { activity ->
            navController =
                TestNavHostController(activity).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
            activity.setContent {
                AfternoteTheme {
                    AppNavigation(
                        startDestination = Route.MindRecord,
                        appState = AppState(navController),
                    )
                }
            }
        }
    }

    @Test
    fun coldEntry_loadsOnlyTheVisibleTab() {
        awaitDailyQuestionTab()

        // 보이는 탭(데일리 질문)만 부른다 — today 1, 목록 1.
        assertEquals(1, dailyQuestion.getTodayCalls)
        assertEquals(1, dailyQuestion.listQueries.size)
        // 일기·주간리포트는 합성되지도 않았으므로 0 이어야 한다. 종전에는 세 VM 을 화면 위에서
        // 호이스팅해 열지도 않은 탭의 init 조회가 진입 즉시 나갔다 (#736).
        assertEquals(0, diary.listQueries.size)
        assertEquals(emptyList<String>(), weekly.requestedDates)
    }

    @Test
    fun jumpToWeeklyTab_doesNotComposeTheDiaryTabInBetween() {
        awaitDailyQuestionTab()

        clickTab(WEEKLY_REPORT_TAB)
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { weekly.requestedDates.isNotEmpty() }
        composeRule.waitForIdle()

        // 0 → 2 는 `scrollToPage` 라 가운데 페이지를 거치지 않는다. `animateScrollToPage` 로
        // 되돌리면 일기 탭이 합성되며 `hiltViewModel()` 이 VM 을 만들어 init 조회가 나간다.
        assertEquals(0, diary.listQueries.size)
        assertEquals(1, weekly.requestedDates.size)
        // 떠난 탭이 다시 부르지도 않는다.
        assertEquals(1, dailyQuestion.getTodayCalls)
        assertEquals(1, dailyQuestion.listQueries.size)
    }

    @Test
    fun resumeWithoutDataChange_addsNoRequests() {
        awaitDailyQuestionTab()
        val todayCalls = dailyQuestion.getTodayCalls
        val listCalls = dailyQuestion.listQueries.size

        // 실제 ON_RESUME 을 발화시킨다 — 화면이 `LifecycleEventEffect(ON_RESUME)` 로 거는
        // 복귀 갱신이 여기서 돈다. 데이터가 그대로면 재조회 가드(#736)가 막아야 한다.
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        assertEquals(todayCalls, dailyQuestion.getTodayCalls)
        assertEquals(listCalls, dailyQuestion.listQueries.size)
        assertEquals(0, diary.listQueries.size)
        assertEquals(emptyList<String>(), weekly.requestedDates)
    }

    /** 탭 텍스트는 하단 네비 라벨과 겹치지 않지만, 클릭 대상만 고르도록 좁힌다. */
    private fun clickTab(label: String) {
        composeRule.onNode(hasText(label) and hasClickAction()).performClick()
    }

    private fun awaitDailyQuestionTab() {
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { dailyQuestion.getTodayCalls > 0 }
        composeRule.waitForIdle()
    }

    private fun emptyWeeklyReport(): WeeklyReport =
        WeeklyReport(
            dailyQuestionAmount = 0,
            diaryAmount = 0,
            summaryText = "",
            week = emptyList(),
            dailyQuestions = emptyList(),
            emotions = emptyList(),
            // 분석 상태는 이 테스트의 관심사가 아니다 — 완료로 고정해 폴링이 끼어들지 않게 한다.
            emotionAnalysis = EmotionAnalysis(total = 0, succeeded = 0, pending = 0, failed = 0),
        )

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val WEEKLY_REPORT_TAB = "주간리포트"
        const val WEEKLY_RESPONSE_SLOTS = 8
    }
}
