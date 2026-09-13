package com.afternote.feature.setting.presentation.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.UserPushSetting
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

            viewModel.onNewsletterToggle(false)

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

            viewModel.onNewsletterToggle(false)
            assertFalse(viewModel.uiState.value.isNewsletterOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isNewsletterOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onSaveFailureDismiss()
            assertNull(viewModel.uiState.value.saveFailure)

            viewModel.onMindRecordToggle(false)
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)
            viewModel.onSaveFailureDismiss()

            viewModel.onAfternoteToggle(false)
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

            viewModel.onMindRecordToggle(false)
            runCurrent()

            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertEquals(PushNotificationSaveFailure.SERVER, viewModel.uiState.value.saveFailure)

            viewModel.onSaveFailureRetry()

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

            viewModel.onAfternoteToggle(false)
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

            viewModel.onNewsletterToggle(false)
            viewModel.onNewsletterToggle(true)

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
    ): PushNotificationViewModel {
        val initial = UserPushSetting(timeLetter = true, mindRecord = true, afterNote = true)
        var remainingFailures = failUpdateAttempts
        val repository =
            FakeUserRepository(pushSetting = initial).apply {
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
            errorReporter = NoOpErrorReporter,
        )
    }
}

private data class PushUpdateCall(
    val timeLetter: Boolean?,
    val mindRecord: Boolean?,
    val afterNote: Boolean?,
)
