package com.afternote.feature.setting.presentation.viewmodel

import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.user.UserPushSetting
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
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
            runCurrent()
            assertFalse(viewModel.uiState.value.isNewsletterOn)
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
            assertTrue(viewModel.uiState.value.showSaveFailure)
            viewModel.onSaveFailureDismiss()
            assertFalse(viewModel.uiState.value.showSaveFailure)

            viewModel.onMindRecordToggle(false)
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isMindRecordOn)
            assertTrue(viewModel.uiState.value.showSaveFailure)
            viewModel.onSaveFailureDismiss()

            viewModel.onAfternoteToggle(false)
            assertFalse(viewModel.uiState.value.isAfternoteOn)
            runCurrent()
            assertTrue(viewModel.uiState.value.isAfternoteOn)
            assertTrue(viewModel.uiState.value.showSaveFailure)

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
            assertTrue(viewModel.uiState.value.showSaveFailure)

            viewModel.onSaveFailureRetry()

            assertFalse(viewModel.uiState.value.showSaveFailure)
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            runCurrent()
            assertFalse(viewModel.uiState.value.isMindRecordOn)
            assertFalse(viewModel.uiState.value.showSaveFailure)
            assertEquals(
                listOf(
                    PushUpdateCall(timeLetter = null, mindRecord = false, afterNote = null),
                    PushUpdateCall(timeLetter = null, mindRecord = false, afterNote = null),
                ),
                calls,
            )
        }

    private fun viewModel(
        calls: MutableList<PushUpdateCall>,
        failUpdateAttempts: Int = 0,
    ): PushNotificationViewModel {
        val initial = UserPushSetting(timeLetter = true, mindRecord = true, afterNote = true)
        var remainingFailures = failUpdateAttempts
        val repository =
            repositoryProxy { methodName, args ->
                when (methodName) {
                    "getMyPushSettings" -> {
                        initial
                    }

                    "updateMyPushSettings" -> {
                        val parameters = checkNotNull(args)
                        calls +=
                            PushUpdateCall(
                                timeLetter = parameters[0] as Boolean?,
                                mindRecord = parameters[1] as Boolean?,
                                afterNote = parameters[2] as Boolean?,
                            )
                        if (remainingFailures > 0) {
                            remainingFailures--
                            throw IllegalStateException("offline")
                        }
                        initial.copy(
                            timeLetter = (parameters[0] as Boolean?) ?: initial.timeLetter,
                            mindRecord = (parameters[1] as Boolean?) ?: initial.mindRecord,
                            afterNote = (parameters[2] as Boolean?) ?: initial.afterNote,
                        )
                    }

                    else -> {
                        error("Unexpected UserRepository call: $methodName")
                    }
                }
            }
        return PushNotificationViewModel(userRepository = repository, deviceAlarmOn = true)
    }

    private fun repositoryProxy(onCall: (String, Array<out Any?>?) -> Any?): UserRepository =
        Proxy.newProxyInstance(
            UserRepository::class.java.classLoader,
            arrayOf(UserRepository::class.java),
        ) { _, method, args -> onCall(method.name, args) } as UserRepository
}

private data class PushUpdateCall(
    val timeLetter: Boolean?,
    val mindRecord: Boolean?,
    val afterNote: Boolean?,
)
