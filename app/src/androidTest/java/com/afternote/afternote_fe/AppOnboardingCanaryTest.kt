package com.afternote.afternote_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.test.FailureArtifactRule
import com.afternote.afternote_fe.test.emptyWeeklyReport
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.Session
import com.afternote.feature.mindrecord.domain.repository.WeeklyReportRepository
import com.afternote.feature.mindrecord.domain.testing.FakeWeeklyReportRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import javax.inject.Inject
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.home.presentation.R as HomeR
import com.afternote.feature.onboarding.presentation.R as OnboardingR

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class AppOnboardingCanaryTest {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var weeklyReportRepository: WeeklyReportRepository

    @Inject
    lateinit var userRepository: UserRepository

    private val fakeAuth get() = authRepository as FakeAuthRepository
    private val fakeUser get() = userRepository as FakeUserRepository
    private val fakeWeeklyReport get() = weeklyReportRepository as FakeWeeklyReportRepository

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot().captureToImage().asAndroidBitmap()
        }

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun inject() {
        hiltRule.inject()
        // 홈이 진입 시 주간 기록 수를 부른다 (#562). 정본 fake 는 큐가 비면 터뜨리므로,
        // 주간 수에 관심이 없는 이 테스트도 기대하는 응답을 명시적으로 넣는다 — 조용히 접으면
        // 요청 횟수가 어긋난 것을 놓친다.
        (weeklyReportRepository as FakeWeeklyReportRepository).results.addLast(
            Result.success(emptyWeeklyReport()),
        )
    }

    @Test
    fun coldStartWithoutSession_opensLoginFromWelcome() {
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_start))
            .assertIsDisplayed()
            .performClick()

        composeRule
            .onNode(
                hasText(context.getString(OnboardingR.string.onboarding_login_top_bar_title)) and
                    hasClickAction(),
            ).assertIsDisplayed()
    }

    @Test
    fun emailLogin_networkFailureThenRetry_entersHomeOnce() {
        val emailLoginResults = ArrayDeque<Result<Session.DefaultSession>>()
        emailLoginResults.addLast(
            Result.failure(CoreAuthFailure.NetworkUnavailable(IOException("offline"))),
        )
        emailLoginResults.addLast(
            Result.success(Session.DefaultSession("access", "refresh")),
        )
        fakeAuth.onDefaultLogin = { _, _ ->
            requireNotNull(emailLoginResults.removeFirstOrNull()) { "email login 응답이 준비되지 않음" }
        }

        openLoginAndEnterCredentials()
        composeRule
            .onNode(hasText(context.getString(OnboardingR.string.onboarding_login_button)) and hasClickAction())
            .performClick()

        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.core_ui_network_error_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(CoreUiR.string.core_ui_network_error_retry))
            .performClick()

        val greeting = context.getString(HomeR.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)
        composeRule.onNodeWithText(greeting).assertIsDisplayed()
        listOf(
            CoreUiR.string.core_ui_nav_item_home,
            CoreUiR.string.core_ui_nav_item_mindrecord,
            CoreUiR.string.core_ui_nav_item_timeletter,
            CoreUiR.string.core_ui_nav_item_note,
        ).forEach { labelRes ->
            // 하단 탭만 고른다 — 홈 섹션 헤더에도 «타임레터» 가 있어 텍스트만으로는 두 개다
            // (#700 이 시안 확정 문구로 그 섹션을 추가했다). 탭은 눌리고 헤더는 안 눌린다.
            composeRule
                .onNode(hasText(context.getString(labelRes)) and hasClickAction())
                .assertIsDisplayed()
        }

        assertEquals(
            listOf("canary@afternote.local" to "password-1234"),
            fakeAuth.attemptedEmailLogins.distinct(),
        )
        assertEquals(2, fakeAuth.attemptedEmailLogins.size)
        assertEquals(1, fakeAuth.saveSessionCalls)
        assertEquals(0, fakeAuth.rotateTokenCalls)
        assertEquals(1, fakeWeeklyReport.requestedDates.size)
        // `logActivityCalls` 단언은 걷는다 — #1413 이 활동 ping 자체를 제거해 develop 에 그 API 가 없다.
    }

    @Test
    fun loginInput_survivesActivityRecreation() {
        openLoginAndEnterCredentials()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNode(hasText("canary@afternote.local")).assertIsDisplayed()
        composeRule.onNode(hasText("password-1234")).assertIsDisplayed()
        assertEquals(0, fakeAuth.attemptedEmailLogins.size)
    }

    private fun openLoginAndEnterCredentials() {
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_start))
            .performClick()
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_login_email_label))
            .performTextInput("canary@afternote.local")
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_login_password_label))
            .performTextInput("password-1234")
    }
}
