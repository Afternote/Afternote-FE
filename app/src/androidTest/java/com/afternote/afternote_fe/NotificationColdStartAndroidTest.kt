package com.afternote.afternote_fe

import android.app.Instrumentation
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.afternote_fe.notification.NotificationEntrySource
import com.afternote.afternote_fe.test.backgroundActivityStartOptions
import com.afternote.core.common.notification.NotificationPendingIntentFactory
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NotificationColdStartAndroidTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

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
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = NotificationEntrySource.FCM
        val occurrenceId = "cold-start-1"
        activityMonitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)

        val pendingIntent =
            NotificationPendingIntentFactory.create(
                context = context,
                source = source.contractValue,
                occurrenceId = occurrenceId,
            )

        assertNotNull(pendingIntent)
        pendingIntent?.send(context, 0, null, null, null, null, backgroundActivityStartOptions())
        launchedActivity =
            activityMonitor?.let { monitor ->
                instrumentation.waitForMonitorWithTimeout(monitor, ACTIVITY_START_TIMEOUT_MILLIS)
            } as? MainActivity
        assertNotNull(launchedActivity)
        instrumentation.waitForIdleSync()

        var pendingEntry: NotificationEntryRequest? = null
        instrumentation.runOnMainSync {
            val activity = requireNotNull(launchedActivity)
            pendingEntry =
                ViewModelProvider(activity)[MainViewModel::class.java]
                    .pendingNotificationEntry
                    .value
        }

        assertEquals(
            NotificationEntryRequest(source = source, occurrenceId = occurrenceId),
            pendingEntry,
        )
    }

    private companion object {
        const val ACTIVITY_START_TIMEOUT_MILLIS = 10_000L
    }
}
