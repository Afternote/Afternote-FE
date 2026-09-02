package com.afternote.afternote_fe

import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
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
import com.afternote.core.common.notification.NotificationDestination
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
    }

    @Test
    fun warmNotificationWhileLoggedOut_isReceivedWithoutBypassingOnboarding() {
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_start))
            .assertIsDisplayed()
        deliverWarmNotification(NotificationEntrySource.FCM, "logged-out-1", NotificationDestination.HOME)

        awaitNotificationIntent(NotificationEntrySource.FCM, "logged-out-1", NotificationDestination.HOME)
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
    fun warmNotificationWhileLoggedIn_isReceivedWithoutRecreatingActivity() {
        fakeAuth.loggedIn = true

        val greeting = context.getString(HomeR.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)
        openTimeLetterTab()
        deliverWarmNotification(NotificationEntrySource.FCM, "logged-in-1", NotificationDestination.HOME)

        awaitNotificationIntent(NotificationEntrySource.FCM, "logged-in-1", NotificationDestination.HOME)
        assertSame(activityUnderTest, resumedMainActivity())
        // 목적지 이동 결선은 #1795 가 Nav3 루트에 붙인다 — 지금은 보던 탭에 머문다.
        awaitSelectedBottomBarTab(CoreUiR.string.core_ui_nav_item_timeletter)
    }

    @Test
    fun backgroundNotification_resumesSameActivityAndKeepsCurrentScreen() {
        fakeAuth.loggedIn = true

        val greeting = context.getString(HomeR.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        deliverWarmNotification(
            NotificationEntrySource.DAILY,
            "background-1",
            NotificationDestination.AFTERNOTE,
        )

        composeRule.waitUntil(timeoutMillis = 10_000) {
            activityUnderTest.lifecycle.currentState == Lifecycle.State.RESUMED
        }
        awaitNotificationIntent(
            NotificationEntrySource.DAILY,
            "background-1",
            NotificationDestination.AFTERNOTE,
        )
        assertSame(activityUnderTest, resumedMainActivity())
        // 목적지 이동 결선은 #1795 가 붙인다 — 지금은 보던 홈 탭에 머문다.
        awaitSelectedBottomBarTab(CoreUiR.string.core_ui_nav_item_home)
    }

    @Test
    fun sameRequestCode_differentSourceAndOccurrenceKeepLatestExtras() {
        deliverWarmNotification(NotificationEntrySource.FCM, "shared-token", NotificationDestination.HOME)
        awaitNotificationIntent(NotificationEntrySource.FCM, "shared-token", NotificationDestination.HOME)

        deliverWarmNotification(
            NotificationEntrySource.DAILY,
            "shared-token",
            NotificationDestination.TIME_LETTER,
        )
        awaitNotificationIntent(
            NotificationEntrySource.DAILY,
            "shared-token",
            NotificationDestination.TIME_LETTER,
        )
    }

    private fun openTimeLetterTab() {
        val timeLetterLabel = context.getString(CoreUiR.string.core_ui_nav_item_timeletter)
        composeRule.onNodeWithText(timeLetterLabel).performClick()
        composeRule.onNode(selectedBottomBarMatcher(CoreUiR.string.core_ui_nav_item_timeletter)).assertIsSelected()
    }

    private fun deliverWarmNotification(
        source: NotificationEntrySource,
        occurrenceToken: String,
        destination: NotificationDestination,
    ) {
        val pendingIntent =
            NotificationPendingIntentFactory.create(
                context = context,
                source = source.contractValue,
                occurrenceToken = occurrenceToken,
                destination = destination,
            )

        assertNotNull(pendingIntent)
        pendingIntent?.send()
    }

    private fun awaitNotificationIntent(
        source: NotificationEntrySource,
        occurrenceToken: String,
        destination: NotificationDestination,
    ) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            activityUnderTest.intent.getStringExtra(
                NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SOURCE,
            ) == source.contractValue &&
                activityUnderTest.intent.getStringExtra(
                    NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN,
                ) == occurrenceToken &&
                activityUnderTest.intent.getStringExtra(
                    NotificationPendingIntentFactory.EXTRA_NOTIFICATION_DESTINATION,
                ) == destination.contractValue
        }
    }

    /** 목적지 이동은 Intent 도착 뒤 한 프레임 더 걸린다 — 선택 탭이 바뀔 때까지 기다린다. */
    private fun awaitSelectedBottomBarTab(labelResource: Int) {
        composeRule.waitUntilAtLeastOneExists(
            selectedBottomBarMatcher(labelResource),
            timeoutMillis = 10_000,
        )
        composeRule.onNode(selectedBottomBarMatcher(labelResource)).assertIsSelected()
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
}
