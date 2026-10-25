package com.afternote.afternote_fe

import android.app.Instrumentation
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.afternote_fe.notification.NotificationEntrySource
import com.afternote.afternote_fe.test.backgroundActivityStartOptions
import com.afternote.core.common.notification.NotificationDestination
import com.afternote.core.common.notification.NotificationPendingIntentFactory
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.domain.testing.FakeAuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.afternote.core.ui.R as CoreUiR

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class NotificationColdStartAndroidTest {
    @Inject
    lateinit var authRepository: AuthRepository

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private var launchedActivity: MainActivity? = null
    private var activityMonitor: Instrumentation.ActivityMonitor? = null

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        launchedActivity?.let { activity ->
            instrumentation.runOnMainSync(activity::finish)
            instrumentation.waitForIdleSync()
        }
        activityMonitor?.let(instrumentation::removeMonitor)
    }

    @Test
    fun coldPendingIntentLaunch_enqueuesInitialNotificationEntry() {
        val source = NotificationEntrySource.FCM
        val occurrenceId = "cold-start-1"
        // 폴백(HOME)이 아닌 목적지를 실어 cold start 경로에서 extra 가 살아남는지 본다.
        val destination = NotificationDestination.TIME_LETTER
        launchNotification(source, occurrenceId, destination)

        var pendingEntry: NotificationEntryRequest? = null
        instrumentation.runOnMainSync {
            val activity = requireNotNull(launchedActivity)
            pendingEntry =
                ViewModelProvider(activity)[MainViewModel::class.java]
                    .pendingNotificationEntry
                    .value
        }

        assertEquals(
            NotificationEntryRequest(
                source = source,
                occurrenceId = occurrenceId,
                destination = destination,
            ),
            pendingEntry,
        )
    }

    @Test
    fun coldPendingIntentLaunchWhileLoggedIn_opensDestination() {
        (authRepository as FakeAuthRepository).loggedIn = true
        launchNotification(NotificationEntrySource.FCM, "cold-start-signed-in", NotificationDestination.TIME_LETTER)

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val destinationTab =
            hasText(context.getString(CoreUiR.string.core_ui_nav_item_timeletter)) and
                SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
        composeRule.waitUntilAtLeastOneExists(destinationTab, timeoutMillis = ACTIVITY_START_TIMEOUT_MILLIS)
        composeRule.onNode(destinationTab).assertIsSelected()
    }

    private fun launchNotification(
        source: NotificationEntrySource,
        occurrenceId: String,
        destination: NotificationDestination,
    ) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        activityMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)

        val pendingIntent =
            NotificationPendingIntentFactory.create(
                context = context,
                source = source.contractValue,
                occurrenceId = occurrenceId,
                destination = destination,
            )

        assertNotNull(pendingIntent)
        pendingIntent?.send(context, 0, null, null, null, null, backgroundActivityStartOptions())
        launchedActivity =
            activityMonitor?.let { monitor ->
                instrumentation.waitForMonitorWithTimeout(monitor, ACTIVITY_START_TIMEOUT_MILLIS)
            } as? MainActivity
        assertNotNull(launchedActivity)
        instrumentation.waitForIdleSync()
    }

    private companion object {
        const val ACTIVITY_START_TIMEOUT_MILLIS = 10_000L
    }
}
