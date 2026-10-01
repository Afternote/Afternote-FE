package com.afternote.feature.setting.presentation.passkey

import android.app.KeyguardManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelStore
import com.afternote.core.common.biometric.BiometricAuthResult
import com.afternote.core.common.biometric.BiometricMessages
import com.afternote.core.common.biometric.authenticateBiometric
import com.afternote.core.datastore.UserProfileDataSource
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager
import org.robolectric.shadows.ShadowBuild

/** Android 10에서 #2202가 바꾼 크래시와 오류 팝업 경로를 실제 AndroidX 구현으로 확인한다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PassKeyMakingApi29Test {
    @get:Rule
    val composeRule = createComposeRule()

    private val viewModelStore = ViewModelStore()
    private var activityController: ActivityController<FragmentActivity>? = null

    @After
    fun tearDown() {
        viewModelStore.clear()
        activityController?.pause()?.stop()?.destroy()
    }

    @Test
    fun `기존 패스키 프롬프트 허용자 조합은 API29에서 예외를 던진다`() {
        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle("패스키 인증")
                    .setSubtitle("본인 인증")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                    .build()
            }

        assertTrue(failure.message.orEmpty().contains("unsupported on API 29"))
    }

    @Test
    fun `공용 래퍼는 API29의 미지원 조합을 크래시 대신 오류로 반환한다`() {
        val activity = secureBiometricActivity()
        val messages =
            BiometricMessages(
                initFailed = "init failed",
                noHardware = "no hardware",
                noneEnrolled = "none enrolled",
                hwUnavailable = "hardware unavailable",
                notAvailable = "unsupported authenticators",
                verificationFailed = "verification failed",
            )

        composeRule.runOnIdle {
            val result = runBlocking { activity.authenticateBiometric("패스키 인증", "본인 인증", messages) }

            assertEquals(BiometricAuthResult.Error(messages.notAvailable), result)
        }
    }

    @Test
    fun `활성화된 지문 인증 버튼은 미지원 오류를 표시하고 패스키를 등록하지 않는다`() {
        val activity = secureBiometricActivity()
        val preferences = RecordingPreferences()
        val viewModel = PassKeyViewModel(UserProfileDataSource(preferences))
        viewModelStore.put("passkey", viewModel)
        var backCalls = 0
        var passwordCalls = 0
        val unavailableMessage = activity.getString(R.string.setting_biometric_not_available)

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides activity) {
                AfternoteTheme {
                    PassKeyMakingScreen(
                        onBackClick = { backCalls++ },
                        onPasswordAuthClick = { passwordCalls++ },
                        viewModel = viewModel,
                    )
                }
            }
        }

        composeRule.onNodeWithText("지문 인증하기").assertIsEnabled().performClick()

        composeRule.onNodeWithText(unavailableMessage).assertIsDisplayed()
        composeRule.onNodeWithText("패스키 생성이 완료되었습니다").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(0, preferences.updateCalls)
            assertEquals(false, viewModel.isPasskeyRegistered.value)
            assertEquals(0, backCalls)
            assertEquals(0, passwordCalls)
        }
        composeRule.onNodeWithText("확인").performClick()
        composeRule.onNodeWithText(unavailableMessage).assertDoesNotExist()
        composeRule.onNodeWithText("지문 인증하기").assertIsEnabled().performClick()
        composeRule.onNodeWithText(unavailableMessage).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, preferences.updateCalls) }
    }

    private fun secureBiometricActivity(): FragmentActivity {
        val activity =
            Robolectric
                .buildActivity(FragmentActivity::class.java)
                .setup()
                .also { activityController = it }
                .get()
        shadowOf(activity.getSystemService(KeyguardManager::class.java)).apply {
            setIsKeyguardSecure(true)
            setIsDeviceSecure(true)
        }
        // AndroidX Biometric 1.1.0의 강한 생체 인증 보장 기기 목록에 있는 모델을 사용한다.
        ShadowBuild.setModel("Pixel 4")
        Shadow
            .extract<ShadowBiometricManager>(
                activity.getSystemService(android.hardware.biometrics.BiometricManager::class.java),
            ).setCanAuthenticate(true)

        // 실제 AVD에서 버튼이 비활성이라 미도달했던 경로를 열린 상태로 검증한다.
        assertEquals(BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG))
        assertEquals(
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
            BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL),
        )
        return activity
    }

    private class RecordingPreferences : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        var updateCalls = 0
            private set

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            updateCalls++
            return transform(data.value).also { data.value = it }
        }
    }
}
