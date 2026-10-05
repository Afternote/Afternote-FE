package com.afternote.feature.setting.presentation.notification

import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 서비스 알림 토글 저장 실패가 개발자 진단으로 남는지 검증한다 (#963).
 *
 * 저장 실패만 [ErrorReporter] 로 올리고 조회 실패·성공·취소는 올리지 않는다. 기록 속성은 단계와
 * 토글 이름뿐이고, [ErrorReporter.recordFailure] 가 덧붙이는 예외 타입 외에는 아무것도 싣지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PushNotificationSaveReportingTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `세 토글의 저장 실패는 토글마다 고정 속성으로 한 번씩 기록하고 롤백과 실패 안내는 그대로다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            val repository =
                FakeSettingNotificationRepository(pushSetting = ALL_ON).apply {
                    onUpdateMyPushSettings = { _, _, _ -> error("server") }
                }
            val viewModel = viewModel(repository, reporter)
            runCurrent()

            viewModel.onNewsletterToggle(false)
            runCurrent()
            assertTrue(viewModel.uiState.value.isNewsletterOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onSaveFailureDismiss()

            viewModel.onMindRecordToggle(false)
            runCurrent()
            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onSaveFailureDismiss()

            viewModel.onAfternoteToggle(false)
            runCurrent()
            assertTrue(viewModel.uiState.value.isAfternoteOn)
            assertFalse(viewModel.uiState.value.isAfternoteUpdating)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)

            assertEquals(
                listOf(saveFailureOf("newsletter"), saveFailureOf("mind_record"), saveFailureOf("afternote")),
                reporter.records.map { it.saveContext() },
            )
            reporter.records.forEach { attributes ->
                assertTrue("허용되지 않은 속성: ${attributes.keys}", ALLOWED_KEYS.containsAll(attributes.keys))
                assertEquals(IllegalStateException::class.java.name, attributes["error_type"])
            }
        }

    @Test
    fun `저장에 성공하면 진단을 남기지 않는다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            val viewModel = viewModel(FakeSettingNotificationRepository(pushSetting = ALL_ON), reporter)
            runCurrent()

            viewModel.onNewsletterToggle(false)
            viewModel.onMindRecordToggle(false)
            viewModel.onAfternoteToggle(false)
            runCurrent()

            assertFalse(viewModel.uiState.value.isNewsletterOn)
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            assertFalse(viewModel.uiState.value.isAfternoteOn)
            assertTrue(reporter.records.isEmpty())
        }

    @Test
    fun `저장 중 화면을 떠나 취소되면 진단을 남기지 않는다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            var cancelled = false
            val repository =
                FakeSettingNotificationRepository(pushSetting = ALL_ON).apply {
                    onUpdateMyPushSettings = { _, _, _ ->
                        suspendCancellableCoroutine { continuation ->
                            continuation.invokeOnCancellation { cancelled = true }
                        }
                    }
                }
            val viewModel = viewModel(repository, reporter)
            val store = ViewModelStore().apply { put(STORE_KEY, viewModel) }
            runCurrent()

            viewModel.onMindRecordToggle(false)
            runCurrent()
            assertEquals(1, repository.pushUpdateCalls.size)

            store.clear()
            runCurrent()

            assertTrue(cancelled)
            assertTrue(reporter.records.isEmpty())
        }

    @Test
    fun `서비스 알림과 마케팅 동의 조회 실패는 진단을 남기지 않는다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            val repository =
                FakeSettingNotificationRepository().apply {
                    onGetMyPushSettings = { error("push settings unavailable") }
                    onGetMyMarketingConsents = { error("marketing consents unavailable") }
                }
            val viewModel = viewModel(repository, reporter)
            runCurrent()

            assertEquals(1, repository.getMyPushSettingsCalls)
            assertEquals(1, repository.getMyMarketingConsentsCalls)
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(reporter.records.isEmpty())
        }

    @Test
    fun `재시도가 다시 실패하면 실제 실패마다 한 번씩 기록한다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            var remainingFailures = 2
            val repository =
                FakeSettingNotificationRepository(pushSetting = ALL_ON).apply {
                    onUpdateMyPushSettings = { _, mindRecord, _ ->
                        if (remainingFailures > 0) {
                            remainingFailures--
                            error("server")
                        }
                        ALL_ON.copy(mindRecord = mindRecord ?: ALL_ON.mindRecord)
                    }
                }
            val viewModel = viewModel(repository, reporter)
            runCurrent()

            viewModel.onMindRecordToggle(false)
            runCurrent()
            viewModel.onSaveFailureRetry()
            runCurrent()
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onSaveFailureRetry()
            runCurrent()

            assertFalse(viewModel.uiState.value.isMindRecordOn)
            assertEquals(3, repository.pushUpdateCalls.size)
            assertEquals(
                List(2) { saveFailureOf("mind_record") },
                reporter.records.map { it.saveContext() },
            )
        }

    private fun viewModel(
        repository: FakeSettingNotificationRepository,
        reporter: ErrorReporter,
    ) = PushNotificationViewModel(
        context = ApplicationProvider.getApplicationContext(),
        notificationRepository = repository,
        errorReporter = reporter,
    )

    /** [ErrorReporter.recordFailure] 가 정책을 적용한 뒤 넘긴 속성을 그대로 모은다. */
    private class RecordingReporter : ErrorReporter {
        val records = mutableListOf<Map<String, String>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            records += attributes
        }
    }

    private fun Map<String, String>.saveContext(): Map<String, String> = filterKeys { it == KEY_STAGE || it == KEY_PUSH_SETTING }

    private fun saveFailureOf(pushSetting: String): Map<String, String> =
        mapOf(KEY_STAGE to "push_setting_update", KEY_PUSH_SETTING to pushSetting)

    private companion object {
        const val STORE_KEY = "push-save-reporting"
        const val KEY_STAGE = "stage"
        const val KEY_PUSH_SETTING = "push_setting"
        val ALLOWED_KEYS = setOf(KEY_STAGE, KEY_PUSH_SETTING, "error_type", "error_cause_type")
        val ALL_ON = UserPushSetting(timeLetter = true, mindRecord = true, afterNote = true)
    }
}
