package com.afternote.feature.setting.presentation.notification

import androidx.test.core.app.ApplicationProvider
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
            advanceUntilIdle()
            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStarted)

            var signals = 0
            MarketingConsent.entries.forEach { consent ->
                viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(consent, false))
                assertFalse(viewModel.uiState.value.checked(consent))
                advanceUntilIdle()

                assertTrue(viewModel.uiState.value.checked(consent))
                if (viewModel.uiState.value.isMarketingConsentSaveFailed) signals++
                viewModel.onIntent(PushNotificationIntent.ConsumeMarketingConsentSaveFailure)
            }

            assertEquals(3, signals)
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
            advanceUntilIdle()
            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStarted)

            MarketingConsent.entries.forEach { consent ->
                viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(consent, false))
            }
            advanceUntilIdle()
            runCurrent()

            assertEquals(
                listOf(false, false, false),
                viewModel.uiState.value.let { listOf(it.isSmsChecked, it.isEmailChecked, it.isPushChecked) },
            )
            assertFalse(viewModel.uiState.value.isMarketingConsentSaveFailed)
            assertTrue(reporter.stages.isEmpty())
        }

    @Test
    fun `STARTED 밖에서 난 실패 안내는 다음 진입에 재생하지 않는다`() =
        runTest(dispatcher) {
            val viewModel = failingViewModel()
            advanceUntilIdle()
            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStarted)
            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStopped)

            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.SMS, false))
            advanceUntilIdle()
            // 값 롤백은 화면 수명과 무관하게 일어난다. 안내만 막힌다.
            assertTrue(viewModel.uiState.value.isSmsChecked)
            assertFalse(viewModel.uiState.value.isMarketingConsentSaveFailed)

            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStarted)
            assertFalse(viewModel.uiState.value.isMarketingConsentSaveFailed)

            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.EMAIL, false))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isMarketingConsentSaveFailed)
        }

    @Test
    fun `소비하기 전에 화면이 멈추면 남은 안내를 걷어 재진입에 띄우지 않는다`() =
        runTest(dispatcher) {
            val viewModel = failingViewModel()
            advanceUntilIdle()
            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStarted)

            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.PUSH, true))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isMarketingConsentSaveFailed)

            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStopped)
            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStarted)

            assertFalse(viewModel.uiState.value.isMarketingConsentSaveFailed)
        }

    @Test
    fun `안내는 소비할 때까지 남고 소비 뒤 연속 실패는 다시 안내한다`() =
        runTest(dispatcher) {
            val reporter = RecordingReporter()
            val viewModel =
                viewModel(
                    repository().apply { onUpdateMyMarketingConsents = { _, _, _ -> error("unavailable") } },
                    reporter,
                )
            advanceUntilIdle()
            viewModel.onIntent(PushNotificationIntent.MarketingFeedbackStarted)

            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.SMS, false))
            advanceUntilIdle()
            // 다른 입력이 지나가도 소비 전까지 신호는 남는다.
            viewModel.onIntent(PushNotificationIntent.RefreshDeviceAlarmStatus)
            assertTrue(viewModel.uiState.value.isMarketingConsentSaveFailed)

            viewModel.onIntent(PushNotificationIntent.ConsumeMarketingConsentSaveFailure)
            assertFalse(viewModel.uiState.value.isMarketingConsentSaveFailed)

            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.SMS, false))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isMarketingConsentSaveFailed)
            assertTrue(viewModel.uiState.value.isSmsChecked)
            assertEquals(listOf("sms_consent_update", "sms_consent_update"), reporter.stages)
        }

    private fun failingViewModel() =
        viewModel(
            repository().apply { onUpdateMyMarketingConsents = { _, _, _ -> error("unavailable") } },
            RecordingReporter(),
        )

    private fun repository() =
        FakeSettingNotificationRepository.strict().apply {
            onGetMyPushSettings = { UserPushSetting(false, false, false) }
            onGetMyMarketingConsents = { UserMarketingConsent(true, true, true) }
        }

    private fun viewModel(
        repository: FakeSettingNotificationRepository,
        reporter: ErrorReporter,
    ) = PushNotificationViewModel(ApplicationProvider.getApplicationContext(), repository, reporter)

    private fun PushNotificationUiState.checked(consent: MarketingConsent): Boolean =
        when (consent) {
            MarketingConsent.SMS -> isSmsChecked
            MarketingConsent.EMAIL -> isEmailChecked
            MarketingConsent.PUSH -> isPushChecked
        }

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
