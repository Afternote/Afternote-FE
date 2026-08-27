package com.afternote.afternote_fe

import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.afternote.afternote_fe.notification.NotificationIntentContract
import com.afternote.afternote_fe.notification.NotificationTopLevelDestination
import com.afternote.core.common.notification.NotificationPendingIntentFactory
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun warmNotificationWhileLoggedOut_isDroppedWithoutBypassingOnboarding() {
        composeRule
            .onNodeWithText(context.getString(OnboardingR.string.onboarding_welcome_start))
            .assertIsDisplayed()

        deliverWarmNotification(NotificationTopLevelDestination.TIME_LETTER, "logged-out-1")

        composeRule.waitForIdle()
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
    fun warmNotificationWhileLoggedIn_navigatesThroughExistingNavHost() {
        fakeAuth.loggedIn = true

        val greeting = context.getString(HomeR.string.home_tab_greeting, "테스트 사용자")
        composeRule.waitUntilAtLeastOneExists(hasText(greeting), timeoutMillis = 10_000)

        deliverWarmNotification(NotificationTopLevelDestination.TIME_LETTER, "logged-in-1")

        val selectedTimeLetter = selectedBottomBarMatcher(CoreUiR.string.core_ui_nav_item_timeletter)
        composeRule.waitUntilAtLeastOneExists(selectedTimeLetter, timeoutMillis = 10_000)
        composeRule.onNode(selectedTimeLetter).assertIsSelected()
    }

    private fun deliverWarmNotification(
        destination: NotificationTopLevelDestination,
        occurrenceToken: String,
    ) {
        val payload =
            Bundle().apply {
                putString(NotificationIntentContract.EXTRA_TARGET, destination.contractValue)
            }
        val notificationIntent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_ENTRY, true)
                putExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_SOURCE, "android-test")
                putExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_OCCURRENCE_TOKEN, occurrenceToken)
                putExtra(NotificationPendingIntentFactory.EXTRA_NOTIFICATION_PAYLOAD, payload)
            }

        composeRule.activityRule.scenario.onActivity { activity ->
            notificationIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            activity.startActivity(notificationIntent)
        }
    }

    private fun selectedBottomBarMatcher(labelResource: Int): SemanticsMatcher =
        hasText(context.getString(labelResource)) and
            SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
}
