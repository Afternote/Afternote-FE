package com.afternote.afternote_fe

import android.content.Intent
import android.os.SystemClock
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.afternote.afternote_fe.notification.NotificationEntrySource
import com.afternote.afternote_fe.test.backgroundActivityStartOptions
import com.afternote.core.common.notification.NotificationPendingIntentFactory
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.afternote.core.ui.R as CoreUiR
import com.afternote.feature.home.presentation.R as HomeR
import com.afternote.feature.onboarding.presentation.R as OnboardingR

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class NotificationNavigationAndroidTest {
    @Inject
    lateinit var authRepository: AuthRepository

    private val fakeAuth get() = authRepository as FakeAuthRepository

    private lateinit var activityUnderTest: MainActivity
    private lateinit var scenarioLaunchIntent: Intent

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun inject() {
        hiltRule.inject()
        activityUnderTest = composeRule.activity
        scenarioLaunchIntent = Intent(activityUnderTest.intent)
    }

    @After
    fun restoreScenarioLaunchIntentAndFinishActivity() {
        if (!::activityUnderTest.isInitialized || !::scenarioLaunchIntent.isInitialized) return

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            activityUnderTest.intent = scenarioLaunchIntent
            activityUnderTest.finish()
        }
        instrumentation.waitForIdleSync()
        awaitActivityDestroyed()
    }

    /**
     * `ActivityScenario`는 대상 Activity를 실행 Intent로 식별해서, 알림 Intent가 들어와 있는 동안의
     * lifecycle 이벤트를 «intent 불일치»로 통째로 무시한다. 그 구간에 RESUMED가 끼면 scenario의
     * stage는 STARTED 같은 전이 stage에 멈춰 있게 되고, steady stage(RESUMED·PAUSED·STOPPED·
     * DESTROYED)가 아닌 채로 rule의 `close()`가 돌면 "Current state was null unexpectedly"로 죽는다.
     * 위에서 Intent를 되돌린 뒤 DESTROYED까지 기다려, scenario가 steady stage를 관측한 상태로
     * teardown에 넘긴다.
     */
    private fun awaitActivityDestroyed() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = SystemClock.uptimeMillis() + ACTIVITY_TEARDOWN_TIMEOUT_MILLIS
        while (SystemClock.uptimeMillis() < deadline) {
            var stage: Stage? = null
            instrumentation.runOnMainSync {
                stage =
                    ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .getLifecycleStageOf(activityUnderTest)
            }
            if (stage == Stage.DESTROYED) return
            instrumentation.waitForIdleSync()
            SystemClock.sleep(ACTIVITY_TEARDOWN_POLL_MILLIS)
        }
    }

    @Test
    fun warmNotificationWhileLoggedOut_isReceivedWithoutBypassingOnboarding() {
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_start))
            .assertIsDisplayed()
        deliverWarmNotification(NotificationEntrySource.FCM, "logged-out-1")

        awaitNotificationIntent(NotificationEntrySource.FCM, "logged-out-1")
        assertSame(activityUnderTest, resumedMainActivity())
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_start))
            .assertIsDisplayed()

        fakeAuth.loggedIn = true

        val greeting = context.getString(HomeR.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)
        composeRule.onNodeWithText(greeting).assertIsDisplayed()
        composeRule.onNode(selectedBottomBarMatcher(CoreUiR.string.core_ui_nav_item_home)).assertIsSelected()
    }

    @Test
    fun warmNotificationWhileLoggedIn_preservesExistingNavHost() {
        fakeAuth.loggedIn = true

        val greeting = context.getString(HomeR.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)
        openTimeLetterTab()
        deliverWarmNotification(NotificationEntrySource.FCM, "logged-in-1")

        awaitNotificationIntent(NotificationEntrySource.FCM, "logged-in-1")
        assertSame(activityUnderTest, resumedMainActivity())
        composeRule.onNode(selectedBottomBarMatcher(CoreUiR.string.core_ui_nav_item_timeletter)).assertIsSelected()
    }

    @Test
    fun backgroundNotification_resumesSameActivityAndKeepsCurrentScreen() {
        fakeAuth.loggedIn = true

        val greeting = context.getString(HomeR.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)
        openTimeLetterTab()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        deliverWarmNotification(NotificationEntrySource.DAILY, "background-1")

        composeRule.waitUntil(timeoutMillis = 10_000) {
            activityUnderTest.lifecycle.currentState == Lifecycle.State.RESUMED
        }
        awaitNotificationIntent(NotificationEntrySource.DAILY, "background-1")
        assertSame(activityUnderTest, resumedMainActivity())
        composeRule.onNode(selectedBottomBarMatcher(CoreUiR.string.core_ui_nav_item_timeletter)).assertIsSelected()
    }

    @Test
    fun sameRequestCode_differentSourceAndOccurrenceKeepLatestExtras() {
        deliverWarmNotification(NotificationEntrySource.FCM, "shared-token")
        awaitNotificationIntent(NotificationEntrySource.FCM, "shared-token")

        deliverWarmNotification(NotificationEntrySource.DAILY, "shared-token")
        awaitNotificationIntent(NotificationEntrySource.DAILY, "shared-token")
    }

    private fun openTimeLetterTab() {
        val timeLetterLabel = context.getString(CoreUiR.string.core_ui_nav_item_timeletter)
        // 하단 탭만 고른다 — 홈 섹션 헤더에도 «타임레터» 가 있어 텍스트만으로는 두 개다
        // (#700 이 시안 확정 문구로 그 섹션을 추가했다). 탭은 눌리고 헤더는 안 눌린다.
        composeRule.onNode(hasText(timeLetterLabel) and hasClickAction()).performClick()
        composeRule.onNode(selectedBottomBarMatcher(CoreUiR.string.core_ui_nav_item_timeletter)).assertIsSelected()
    }

    private fun deliverWarmNotification(
        source: NotificationEntrySource,
        occurrenceId: String,
    ) {
        val pendingIntent =
            NotificationPendingIntentFactory.create(
                context = context,
                source = source.contractValue,
                occurrenceId = occurrenceId,
            )

        assertNotNull(pendingIntent)
        pendingIntent?.send(context, 0, null, null, null, null, backgroundActivityStartOptions())
    }

    private fun awaitNotificationIntent(
        source: NotificationEntrySource,
        occurrenceId: String,
    ) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            activityUnderTest.intent.getStringExtra(
                NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SOURCE,
            ) == source.contractValue &&
                activityUnderTest.intent.getStringExtra(
                    NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN,
                ) == occurrenceId
        }
    }

    private fun resumedMainActivity(): MainActivity {
        var resumedActivity: MainActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            resumedActivity =
                ActivityLifecycleMonitorRegistry
                    .getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .filterIsInstance<MainActivity>()
                    .singleOrNull()
        }
        return requireNotNull(resumedActivity) { "MainActivity is not the only resumed activity" }
    }

    private fun selectedBottomBarMatcher(labelResource: Int): SemanticsMatcher =
        hasText(context.getString(labelResource)) and
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)

    private companion object {
        const val ACTIVITY_TEARDOWN_TIMEOUT_MILLIS = 10_000L
        const val ACTIVITY_TEARDOWN_POLL_MILLIS = 50L
    }
}
