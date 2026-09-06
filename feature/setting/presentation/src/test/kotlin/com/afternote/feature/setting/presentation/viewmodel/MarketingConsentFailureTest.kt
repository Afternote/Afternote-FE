package com.afternote.feature.setting.presentation.viewmodel

import androidx.test.core.app.ApplicationProvider
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.testing.FakeUserRepository
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MarketingConsentFailureTest {
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
    fun `각 동의 철회 실패는 값을 복원하고 안내와 진단을 남긴다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            val repository =
                repository().apply {
                    onUpdateMyMarketingConsents = { _, _, _ -> error("unavailable") }
                }
            val viewModel = viewModel(repository, reporter)
            val events = mutableListOf<PushNotificationEvent>()
            backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
            advanceUntilIdle()
            runCurrent()

            viewModel.onSmsChecked(false)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isSmsChecked)
            viewModel.onEmailChecked(false)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isEmailChecked)
            viewModel.onPushChecked(false)
            advanceUntilIdle()
            runCurrent()
            assertTrue(viewModel.uiState.value.isPushChecked)

            assertEquals(List(3) { PushNotificationEvent.MarketingConsentSaveFailed }, events)
            assertEquals(listOf("sms_consent_update", "email_consent_update", "push_consent_update"), reporter.stages)
            assertEquals(
                listOf(Triple(false, null, null), Triple(null, false, null), Triple(null, null, false)),
                repository.marketingConsentUpdates,
            )
        }

    @Test
    fun `각 동의 저장 취소는 롤백이나 실패 안내로 처리하지 않는다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            val repository =
                repository().apply {
                    onUpdateMyMarketingConsents = { _, _, _ -> throw CancellationException("screen left") }
                }
            val viewModel = viewModel(repository, reporter)
            val events = mutableListOf<PushNotificationEvent>()
            backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
            advanceUntilIdle()
            runCurrent()

            viewModel.onSmsChecked(false)
            viewModel.onEmailChecked(false)
            viewModel.onPushChecked(false)
            advanceUntilIdle()
            runCurrent()

            assertEquals(
                listOf(false, false, false),
                viewModel.uiState.value.let { listOf(it.isSmsChecked, it.isEmailChecked, it.isPushChecked) },
            )
            assertTrue(events.isEmpty())
            assertTrue(reporter.stages.isEmpty())
        }

    @Test
    fun `구독이 끝난 동안의 실패 안내는 다음 구독에 재생하지 않는다`() =
        runTest(dispatcher) {
            val viewModel =
                viewModel(
                    repository().apply {
                        onUpdateMyMarketingConsents = { _, _, _ -> error("unavailable") }
                    },
                    RecordingReporter(),
                )
            val events = mutableListOf<PushNotificationEvent>()
            val firstCollector = backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
            advanceUntilIdle()
            runCurrent()
            firstCollector.cancel()
            runCurrent()

            viewModel.onSmsChecked(false)
            advanceUntilIdle()
            backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
            runCurrent()
            assertTrue(events.isEmpty())

            viewModel.onEmailChecked(false)
            advanceUntilIdle()
            runCurrent()
            assertEquals(listOf(PushNotificationEvent.MarketingConsentSaveFailed), events)
        }

    private fun repository() =
        FakeUserRepository.strict().apply {
            onGetMyPushSettings = { UserPushSetting(false, false, false) }
            onGetMyMarketingConsents = { UserMarketingConsent(true, true, true) }
        }

    private fun viewModel(
        repository: FakeUserRepository,
        reporter: ErrorReporter,
    ) = PushNotificationViewModel(ApplicationProvider.getApplicationContext(), repository, reporter)

    private class RecordingReporter : ErrorReporter {
        val stages = mutableListOf<String?>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            stages += attributes["stage"]
        }
    }
}
