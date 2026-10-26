package com.afternote.feature.setting.presentation.notification

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.afternote.core.model.user.UserMarketingConsent
import com.afternote.core.model.user.UserPushSetting
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import com.afternote.feature.setting.presentation.NoOpErrorReporter
import com.afternote.feature.setting.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 알림 설정 화면이 마케팅 동의 저장 실패 신호를 받아 안내하고 소비하는지, STARTED 밖의 실패를
 * 재진입에 재생하지 않는지 화면 배선까지 확인한다 (#1502, #558).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationSettingSignalTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val failedMessage = context.getString(R.string.setting_marketing_consent_save_failed)
    private val repository =
        FakeSettingNotificationRepository.strict().apply {
            onGetMyPushSettings = { UserPushSetting(timeLetter = false, mindRecord = false, afterNote = false) }
            onGetMyMarketingConsents = { UserMarketingConsent(sms = true, email = true, push = true) }
            onUpdateMyMarketingConsents = { _, _, _ -> error("unavailable") }
        }
    private val viewModel = PushNotificationViewModel(context, repository, NoOpErrorReporter)
    private val lifecycleOwner = ManualLifecycleOwner()

    @Test
    fun `저장 실패 신호를 받으면 안내를 띄우고 신호를 소비한다`() {
        setScreen()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.uiState.value.isMarketingFeedbackActive }

        composeRule.runOnIdle {
            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.SMS, false))
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(failedMessage).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(failedMessage).assertExists()
        assertFalse(viewModel.uiState.value.isMarketingConsentSaveFailed)
        assertTrue(viewModel.uiState.value.isSmsChecked)
    }

    @Test
    fun `STARTED 밖에서 난 저장 실패는 다시 시작해도 안내하지 않는다`() {
        setScreen()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.uiState.value.isMarketingFeedbackActive }

        composeRule.runOnIdle { lifecycleOwner.moveTo(Lifecycle.State.CREATED) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { !viewModel.uiState.value.isMarketingFeedbackActive }

        composeRule.runOnIdle {
            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.EMAIL, false))
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.marketingConsentUpdates.size == 1 && viewModel.uiState.value.isEmailChecked
        }
        assertFalse(viewModel.uiState.value.isMarketingConsentSaveFailed)

        composeRule.runOnIdle { lifecycleOwner.moveTo(Lifecycle.State.RESUMED) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.uiState.value.isMarketingFeedbackActive }
        composeRule.waitForIdle()

        assertTrue(composeRule.onAllNodesWithText(failedMessage).fetchSemanticsNodes().isEmpty())
        assertFalse(viewModel.uiState.value.isMarketingConsentSaveFailed)
    }

    @Test
    fun `안내 중에 화면이 멈추면 떠 있던 안내와 밀린 안내를 거둬 재진입에 띄우지 않는다`() {
        setScreen()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.uiState.value.isMarketingFeedbackActive }

        // 첫 실패의 안내가 떠 있는 동안 둘째 실패가 나서 그 뒤에 줄을 선다.
        composeRule.runOnIdle {
            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.SMS, false))
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { isFailedMessageShown() }
        composeRule.runOnIdle {
            viewModel.onIntent(PushNotificationIntent.ChangeMarketingConsent(MarketingConsent.EMAIL, false))
        }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.marketingConsentUpdates.size == 2 &&
                viewModel.uiState.value.isEmailChecked &&
                !viewModel.uiState.value.isMarketingConsentSaveFailed
        }

        // 안내 시간이 흘러 저절로 닫히지 않게 시계를 세운다. 멈췄다 돌아오는 것만으로 걷히는지 본다.
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { lifecycleOwner.moveTo(Lifecycle.State.CREATED) }
        composeRule.runOnIdle { lifecycleOwner.moveTo(Lifecycle.State.RESUMED) }
        composeRule.mainClock.advanceTimeBy(SNACKBAR_EXIT_MILLIS)

        assertFalse(isFailedMessageShown())
    }

    @Test
    fun `알림 설정 화면 첫 진입은 중복 조회하지 않고 복귀하면 서버 동의를 갱신한다`() {
        setScreen()
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) { viewModel.uiState.value.isMarketingFeedbackActive }
        composeRule.waitForIdle()
        assertEquals(1, repository.getMyMarketingConsentsCalls)
        composeRule.runOnIdle {
            lifecycleOwner.moveTo(Lifecycle.State.CREATED)
            repository.onGetMyMarketingConsents = { UserMarketingConsent(sms = false, email = false, push = false) }
        }
        composeRule.runOnIdle { lifecycleOwner.moveTo(Lifecycle.State.RESUMED) }
        composeRule.waitUntil(timeoutMillis = TIMEOUT_MILLIS) {
            repository.getMyMarketingConsentsCalls == 2 && !viewModel.uiState.value.isSmsChecked
        }
        assertFalse(viewModel.uiState.value.isEmailChecked)
        assertFalse(viewModel.uiState.value.isPushChecked)
    }

    private fun isFailedMessageShown(): Boolean = composeRule.onAllNodesWithText(failedMessage).fetchSemanticsNodes().isNotEmpty()

    private fun setScreen() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                AfternoteTheme {
                    NotificationSettingScreen(
                        onBack = {},
                        onPushNotificationClick = {},
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    /** 화면이 보는 수명만 따로 움직인다. 호스트 액티비티는 RESUMED 로 둔 채 STARTED 밖을 흉내 낸다. */
    private class ManualLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }

        override val lifecycle: Lifecycle get() = registry

        fun moveTo(state: Lifecycle.State) {
            registry.currentState = state
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L

        /** 스낵바가 사라지는 애니메이션이 끝나기에 넉넉하고, 짧은 안내 시간(4초)보다는 짧다. */
        const val SNACKBAR_EXIT_MILLIS = 1_000L
    }
}
