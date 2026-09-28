package com.afternote.feature.setting.presentation.notification

import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import com.afternote.feature.setting.presentation.NoOpErrorReporter
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
            val viewModel = viewModel(calls = calls)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, false))

            assertFalse(viewModel.uiState.value.isNewsletterOn)
            assertTrue(viewModel.uiState.value.isNewsletterUpdating)
            runCurrent()
            assertFalse(viewModel.uiState.value.isNewsletterOn)
            assertFalse(viewModel.uiState.value.isNewsletterUpdating)
            assertEquals(
                listOf(PushUpdateCall(timeLetter = false, mindRecord = null, afterNote = null)),
                calls,
            )
        }

    @Test
    fun `각 토글 저장 실패 시 이전 값으로 롤백하고 실패 안내를 표시한다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val viewModel = viewModel(calls = calls, failUpdateAttempts = Int.MAX_VALUE)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, false))
            assertFalse(viewModel.uiState.value.isNewsletterOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isNewsletterOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onIntent(PushNotificationIntent.DismissSaveFailure)
            assertNull(viewModel.uiState.value.saveFailure)

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.MIND_RECORD, false))
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onIntent(PushNotificationIntent.DismissSaveFailure)

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.AFTERNOTE, false))
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
        }

    @Test
    fun `저장 실패 안내에서 재시도하면 마지막 변경을 다시 저장한다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val viewModel = viewModel(calls = calls, failUpdateAttempts = 1)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.MIND_RECORD, false))
            runCurrent()

            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)

            viewModel.onIntent(PushNotificationIntent.RetrySave)

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

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.AFTERNOTE, false))
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

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, false))
            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, true))

            assertFalse(viewModel.uiState.value.isNewsletterOn)
            assertTrue(viewModel.uiState.value.isNewsletterUpdating)
            runCurrent()
            assertEquals(
                listOf(PushUpdateCall(timeLetter = false, mindRecord = null, afterNote = null)),
                calls,
            )
            assertFalse(viewModel.uiState.value.isNewsletterUpdating)
        }

    @Test
    fun `재시도를 연달아 보내도 실패한 변경을 한 번만 다시 저장한다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val viewModel = viewModel(calls = calls, failUpdateAttempts = 1)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.AFTERNOTE, false))
            runCurrent()
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)

            viewModel.onIntent(PushNotificationIntent.RetrySave)
            viewModel.onIntent(PushNotificationIntent.RetrySave)
            assertTrue(viewModel.uiState.value.isAfternoteUpdating)
            runCurrent()

            assertEquals(
                List(2) { PushUpdateCall(timeLetter = null, mindRecord = null, afterNote = false) },
                calls,
            )
            assertFalse(viewModel.uiState.value.isAfternoteOn)
            assertFalse(viewModel.uiState.value.isAfternoteUpdating)
            assertNull(viewModel.uiState.value.failedUpdate)
        }

    @Test
    fun `실패 안내를 닫으면 재시도 대상도 비워 이후 재시도가 요청을 보내지 않는다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val viewModel = viewModel(calls = calls, failUpdateAttempts = 1)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, false))
            runCurrent()
            viewModel.onIntent(PushNotificationIntent.DismissSaveFailure)

            assertNull(viewModel.uiState.value.saveFailure)
            assertNull(viewModel.uiState.value.failedUpdate)

            viewModel.onIntent(PushNotificationIntent.RetrySave)
            runCurrent()

            assertEquals(listOf(PushUpdateCall(timeLetter = false, mindRecord = null, afterNote = null)), calls)
            assertTrue(viewModel.uiState.value.isNewsletterOn)
        }

    @Test
    fun `서로 다른 토글이 연달아 실패하면 재시도 대상은 마지막 실패만 남는다`() =
        runTest(dispatcher) {
            val calls = mutableListOf<PushUpdateCall>()
            val viewModel = viewModel(calls = calls, failUpdateAttempts = 2)
            runCurrent()

            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, false))
            viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.MIND_RECORD, false))
            runCurrent()

            assertTrue(viewModel.uiState.value.isNewsletterOn)
            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)

            viewModel.onIntent(PushNotificationIntent.RetrySave)
            runCurrent()

            assertEquals(PushUpdateCall(timeLetter = null, mindRecord = false, afterNote = null), calls.last())
            assertEquals(3, calls.size)
            assertTrue(viewModel.uiState.value.isNewsletterOn)
            assertFalse(viewModel.uiState.value.isMindRecordOn)
        }

    private fun viewModel(
        calls: MutableList<PushUpdateCall>,
        failUpdateAttempts: Int = 0,
        updateFailure: Throwable = IllegalStateException("server"),
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
            notificationRepository = repository,
            errorReporter = NoOpErrorReporter,
        )
    }
}

private data class PushUpdateCall(
    val timeLetter: Boolean?,
    val mindRecord: Boolean?,
    val afterNote: Boolean?,
)
