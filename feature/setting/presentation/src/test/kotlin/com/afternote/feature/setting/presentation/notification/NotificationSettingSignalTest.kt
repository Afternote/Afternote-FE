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
    }
}
