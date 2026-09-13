package com.afternote.feature.setting.presentation.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import com.afternote.feature.setting.presentation.NoOpErrorReporter
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationIntent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PushNotificationViewModelTest {
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
    fun `뉴스레터 저장 성공 시 낙관적 변경을 유지하고 정확한 값을 전송한다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val reporter = RecordingReporter()
            val viewModel = viewModel(calls = calls, errorReporter = reporter)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.NewsletterToggle(false))

            assertFalse(viewModel.uiState.value.isNewsletterOn)
            assertTrue(viewModel.uiState.value.isNewsletterUpdating)
            runCurrent()
            assertFalse(viewModel.uiState.value.isNewsletterOn)
            assertFalse(viewModel.uiState.value.isNewsletterUpdating)
            assertEquals(
                listOf(PushUpdateCall(timeLetter = false, mindRecord = null, afterNote = null)),
                calls,
            )
            assertTrue(reporter.failures.isEmpty())
        }

    @Test
    fun `각 토글 저장 실패 시 이전 값으로 롤백하고 실패 안내를 표시한다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val reporter = RecordingReporter()
            val viewModel =
                viewModel(
                    calls = calls,
                    failUpdateAttempts = Int.MAX_VALUE,
                    updateFailure = IllegalStateException("email=user@example.com", IOException("token=private")),
                    errorReporter = reporter,
                )
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.NewsletterToggle(false))
            assertFalse(viewModel.uiState.value.isNewsletterOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isNewsletterOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onIntent(PushNotificationIntent.SaveFailureDismiss)
            assertNull(viewModel.uiState.value.saveFailure)

            viewModel.onIntent(PushNotificationIntent.MindRecordToggle(false))
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onIntent(PushNotificationIntent.SaveFailureDismiss)

            viewModel.onIntent(PushNotificationIntent.AfternoteToggle(false))
            assertFalse(viewModel.uiState.value.isAfternoteOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isAfternoteOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)

            assertEquals(
                listOf(
                    PushUpdateCall(timeLetter = false, mindRecord = null, afterNote = null),
                    PushUpdateCall(timeLetter = null, mindRecord = false, afterNote = null),
                    PushUpdateCall(timeLetter = null, mindRecord = null, afterNote = false),
                ),
                calls,
            )
            assertEquals(
                listOf("newsletter", "mind_record", "afternote"),
                reporter.failures.map { it.second["push_setting"] },
            )
            reporter.failures.forEach { (failure, attributes) ->
                assertEquals(
                    mapOf(
                        "stage" to "push_setting_update",
                        "push_setting" to attributes.getValue("push_setting"),
                        "error_type" to IllegalStateException::class.java.name,
                        "error_cause_type" to IOException::class.java.name,
                    ),
                    attributes,
                )
                assertEquals(IllegalStateException::class.java.name, failure.message)
                assertNull(failure.cause)
            }
        }

    @Test
    fun `저장 실패 안내에서 재시도하면 마지막 변경을 다시 저장한다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val reporter = RecordingReporter()
            val viewModel = viewModel(calls = calls, failUpdateAttempts = 1, errorReporter = reporter)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.MindRecordToggle(false))
            runCurrent()

            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)

            viewModel.onIntent(PushNotificationIntent.SaveFailureRetry)

            assertNull(viewModel.uiState.value.saveFailure)
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            runCurrent()
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            assertNull(viewModel.uiState.value.saveFailure)
            assertEquals(
                listOf(
                    PushUpdateCall(timeLetter = null, mindRecord = false, afterNote = null),
                    PushUpdateCall(timeLetter = null, mindRecord = false, afterNote = null),
                ),
                calls,
            )
            assertEquals(1, reporter.failures.size)
        }

    @Test
    fun `서비스 알림 저장 취소는 실패 안내나 진단을 남기지 않는다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            val calls = mutableListOf<PushUpdateCall>()
            val viewModel =
                viewModel(
                    calls = calls,
                    failUpdateAttempts = Int.MAX_VALUE,
                    updateFailure = CancellationException("screen left"),
                    errorReporter = reporter,
                )
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.NewsletterToggle(false))
            viewModel.onIntent(PushNotificationIntent.MindRecordToggle(false))
            viewModel.onIntent(PushNotificationIntent.AfternoteToggle(false))
            runCurrent()

            assertEquals(3, calls.size)
            assertFalse(viewModel.uiState.value.isNewsletterOn)
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            assertFalse(viewModel.uiState.value.isAfternoteOn)
            assertNull(viewModel.uiState.value.saveFailure)
            assertTrue(reporter.failures.isEmpty())
        }

    @Test
    fun `네트워크 저장 실패는 네트워크 오류 안내로 구분한다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val viewModel =
                viewModel(
                    calls = calls,
                    failUpdateAttempts = 1,
                    updateFailure = PushSettingFailure.NetworkUnavailable(IOException("offline")),
                )
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.AfternoteToggle(false))
            runCurrent()

            assertEquals(PushNotificationSaveFailure.NETWORK, viewModel.uiState.value.saveFailure)
            assertTrue(viewModel.uiState.value.isAfternoteOn)
            assertFalse(viewModel.uiState.value.isAfternoteUpdating)
        }

    @Test
    fun `저장 중 같은 토글을 다시 변경해도 중복 요청하지 않는다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val viewModel = viewModel(calls = calls)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.NewsletterToggle(false))
            viewModel.onIntent(PushNotificationIntent.NewsletterToggle(true))

            assertFalse(viewModel.uiState.value.isNewsletterOn)
            assertTrue(viewModel.uiState.value.isNewsletterUpdating)
            runCurrent()
            assertEquals(
                listOf(PushUpdateCall(timeLetter = false, mindRecord = null, afterNote = null)),
                calls,
            )
            assertFalse(viewModel.uiState.value.isNewsletterUpdating)
        }

    private fun viewModel(
        calls: MutableList<PushUpdateCall>,
        failUpdateAttempts: Int = 0,
        updateFailure: Throwable = IllegalStateException("server"),
        errorReporter: ErrorReporter = NoOpErrorReporter,
    ): PushNotificationViewModel {
        val initial = UserPushSetting(timeLetter = true, mindRecord = true, afterNote = true)
        var remainingFailures = failUpdateAttempts
        val repository =
            FakeSettingNotificationRepository(pushSetting = initial).apply {
                onUpdateMyPushSettings = { timeLetter, mindRecord, afterNote ->
                    calls += PushUpdateCall(timeLetter, mindRecord, afterNote)
                    if (remainingFailures > 0) {
                        remainingFailures--
                        throw updateFailure
                    }
                    initial.copy(
                        timeLetter = timeLetter ?: initial.timeLetter,
                        mindRecord = mindRecord ?: initial.mindRecord,
                        afterNote = afterNote ?: initial.afterNote,
                    )
                }
            }
        return PushNotificationViewModel(
            context = ApplicationProvider.getApplicationContext(),
            userRepository = repository,
            errorReporter = errorReporter,
        )
    }

    private class RecordingReporter : ErrorReporter {
        val failures = mutableListOf<Pair<Throwable, Map<String, String>>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            failures += throwable to attributes
        }
    }
}

private data class PushUpdateCall(
    val timeLetter: Boolean?,
    val mindRecord: Boolean?,
    val afterNote: Boolean?,
)
