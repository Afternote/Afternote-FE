package com.afternote.feature.setting.presentation.passkey

import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.afternote.core.common.biometric.BiometricAuthResult
import com.afternote.core.common.biometric.BiometricMessages
import com.afternote.core.common.biometric.authenticateBiometric
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.domain.push.DevicePushTargetProvider
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.setting.domain.Passkey
import com.afternote.feature.setting.domain.PasskeyRepository
import com.afternote.feature.setting.domain.SettingAccountRepository
import com.afternote.feature.setting.domain.SettingNotificationRepository
import com.afternote.feature.setting.domain.testing.FakeSettingAccountRepository
import com.afternote.feature.setting.domain.testing.FakeSettingNotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.ClassName
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowFingerprintManager
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/** AndroidX의 실제 사전 검사와 프롬프트 구성을 통과한 뒤 framework 콜백을 전달한다. */
@RunWith(RobolectricTestRunner::class)
@HiltAndroidTest
@Config(
    sdk = [29],
    application = HiltTestApplication::class,
    shadows = [RecordingBiometricPrompt::class, RecordingFingerprintManager::class],
)
class PassKeyMakingApi29Test {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @BindValue
    val passkeyRepository: PasskeyRepository =
        object : PasskeyRepository {
            override suspend fun getPasskeys(): List<Passkey> = error("생체 인증 오류 경로는 목록을 조회하지 않는다")

            override suspend fun getRegistrationOptions(): String = error("생체 인증 오류 경로는 등록 옵션을 조회하지 않는다")

            override suspend fun registerPasskey(credentialJson: String): Passkey = error("생체 인증 오류 경로는 등록하지 않는다")
        }

    @BindValue
    val accountRepository: SettingAccountRepository = FakeSettingAccountRepository.strict()

    @BindValue
    val notificationRepository: SettingNotificationRepository = FakeSettingNotificationRepository.strict()

    @BindValue
    val pushTargetProvider: DevicePushTargetProvider =
        object : DevicePushTargetProvider {
            override suspend fun currentTargetId(): String? = error("생체 인증 오류 경로는 푸시 대상을 조회하지 않는다")

            override suspend fun existingTargetId(): String? = error("생체 인증 오류 경로는 푸시 대상을 조회하지 않는다")
        }

    @BindValue
    val errorReporter: ErrorReporter =
        object : ErrorReporter {
            override fun writeFailure(
                throwable: Throwable,
                attributes: Map<String, String>,
            ) {
                error("생체 인증 오류 경로에서 예상하지 않은 오류 보고")
            }
        }

    @Inject
    lateinit var profileCache: UserProfileCacheRepository

    private var activityController: ActivityController<BiometricTestActivity>? = null

    @Before
    fun setUp() {
        RecordedBiometricSession.reset()
        hiltRule.inject()
        runBlocking { profileCache.savePasskeyRegistered(false) }
    }

    @After
    fun tearDown() {
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
    @Config(sdk = [26, 27, 28, 29, 30, 35])
    fun `공용 래퍼는 지원되는 강도와 취소 정책으로 프롬프트를 열고 사용자 취소를 전달한다`() {
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
        lateinit var result: Deferred<BiometricAuthResult>

        composeRule.runOnIdle {
            result =
                CoroutineScope(Dispatchers.Main.immediate).async(start = CoroutineStart.UNDISPATCHED) {
                    activity.authenticateBiometric("패스키 인증", "본인 인증", messages)
                }
        }
        composeRule.waitUntil { RecordedBiometricSession.authenticationStarts == 1 }
        composeRule.runOnIdle {
            val fragment = activity.supportFragmentManager.fragments.single { it.javaClass.simpleName == "BiometricFragment" }
            val biometricViewModel = ReflectionHelpers.getField<Any>(fragment, "mViewModel")
            // AndroidX가 실제로 받아 보관한 PromptInfo를 읽는다. 앱 선언의 공개 범위는 바꾸지 않는다.
            val promptInfo = ReflectionHelpers.getField<BiometricPrompt.PromptInfo>(biometricViewModel, "mPromptInfo")
            val biometricOnly = Build.VERSION.SDK_INT in 28..29
            assertEquals(
                if (biometricOnly) BIOMETRIC_STRONG else BIOMETRIC_STRONG or DEVICE_CREDENTIAL,
                promptInfo.allowedAuthenticators,
            )
            assertEquals(
                if (biometricOnly) activity.getString(android.R.string.cancel) else "",
                promptInfo.negativeButtonText.toString(),
            )
            assertFalse(result.isCompleted)
            RecordedBiometricSession.error(BiometricPrompt.ERROR_USER_CANCELED, "user canceled")
        }
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        composeRule.waitUntil { result.isCompleted }
        assertEquals(BiometricAuthResult.Canceled, runBlocking { result.await() })
        assertEquals(false, runBlocking { profileCache.isPasskeyRegisteredFlow().first() })
    }

    @Test
    @Config(sdk = [28])
    fun `API28 Samsung crypto 인증은 AppCompat 지문 대화상자를 열고 취소할 수 있다`() {
        val activity = secureBiometricActivity()
        ShadowBuild.setManufacturer("samsung")
        ShadowBuild.setModel("SM-G960F")
        // Robolectric의 AndroidKeyStore 대신 일반 cipher로 실제 AndroidX crypto 경로를 연다.
        val cipher =
            Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(ByteArray(16), "AES"))
            }
        var errorCode: Int? = null
        composeRule.runOnIdle {
            val prompt =
                BiometricPrompt(
                    activity,
                    Executor { it.run() },
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationError(
                            code: Int,
                            errString: CharSequence,
                        ) {
                            errorCode = code
                        }
                    },
                )
            val promptInfo =
                BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle("패스키 인증")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                    .setNegativeButtonText(activity.getString(android.R.string.cancel))
                    .build()
            prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
        composeRule.waitUntil { RecordedBiometricSession.authenticationStarts == 1 }
        composeRule.runOnIdle {
            val dialog =
                activity.supportFragmentManager.fragments.single { it.javaClass.simpleName == "FingerprintDialogFragment" }
                    as DialogFragment
            assertTrue(dialog.requireDialog().isShowing)
            RecordedBiometricSession.error(BiometricPrompt.ERROR_USER_CANCELED, "user canceled")
        }
        shadowOf(Looper.getMainLooper()).idle()
        composeRule.waitForIdle()
        assertEquals(BiometricPrompt.ERROR_USER_CANCELED, errorCode)
        assertEquals(false, runBlocking { profileCache.isPasskeyRegisteredFlow().first() })
    }

    @Test
    fun `API29 지문 불일치와 사용자 취소는 패스키 저장이나 화면 이동을 하지 않는다`() {
        val activity = secureBiometricActivity()
        val viewModel = showPasskeyScreen(activity)
        composeRule.onNodeWithText("지문 인증하기").assertIsEnabled().performClick()
        composeRule.waitUntil { RecordedBiometricSession.authenticationStarts == 1 }

        composeRule.runOnIdle { RecordedBiometricSession.failure() }
        assertNotRegistered(viewModel)
        composeRule.runOnIdle { RecordedBiometricSession.error(BiometricPrompt.ERROR_USER_CANCELED, "user canceled") }
        composeRule.onNodeWithText("지문 인증하기").assertIsEnabled()
        composeRule.onNodeWithText("user canceled").assertDoesNotExist()
        assertNotRegistered(viewModel)
    }

    @Test
    fun `API29 실제 프롬프트의 오류를 닫고 재시도해도 실패는 패스키를 등록하지 않는다`() {
        val activity = secureBiometricActivity()
        val viewModel = showPasskeyScreen(activity)
        val errorMessage = "sensor unavailable"
        composeRule.onNodeWithText("지문 인증하기").assertIsEnabled().performClick()
        composeRule.waitUntil { RecordedBiometricSession.authenticationStarts == 1 }
        composeRule.runOnIdle { RecordedBiometricSession.error(BiometricPrompt.ERROR_HW_UNAVAILABLE, errorMessage) }

        composeRule.onNodeWithText(errorMessage).assertIsDisplayed()
        assertNotRegistered(viewModel)
        composeRule.onNodeWithText("확인").performClick()
        composeRule.onNodeWithText(errorMessage).assertDoesNotExist()
        composeRule.onNodeWithText("지문 인증하기").assertIsEnabled().performClick()
        composeRule.waitUntil { RecordedBiometricSession.authenticationStarts == 2 }
        composeRule.runOnIdle { RecordedBiometricSession.error(BiometricPrompt.ERROR_HW_UNAVAILABLE, errorMessage) }
        composeRule.onNodeWithText(errorMessage).assertIsDisplayed()
        assertNotRegistered(viewModel)
    }

    private var backCalls = 0
    private var passwordCalls = 0

    private fun showPasskeyScreen(activity: FragmentActivity): PassKeyViewModel {
        val viewModel = ViewModelProvider(activity)[PassKeyViewModel::class.java]
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
        return viewModel
    }

    private fun assertNotRegistered(viewModel: PassKeyViewModel) {
        composeRule.onNodeWithText("패스키 생성이 완료되었습니다").assertDoesNotExist()
        composeRule.runOnIdle {
            assertEquals(null, viewModel.uiState.value.result)
            assertFalse(viewModel.uiState.value.isRegistering)
            assertEquals(0, backCalls)
            assertEquals(0, passwordCalls)
        }
        assertEquals(false, runBlocking { profileCache.isPasskeyRegisteredFlow().first() })
    }

    private fun secureBiometricActivity(): FragmentActivity {
        val activity =
            Robolectric
                .buildActivity(BiometricTestActivity::class.java)
                .also { it.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat) }
                .setup()
                .also { activityController = it }
                .get()
        shadowOf(activity.getSystemService(KeyguardManager::class.java)).apply {
            setIsKeyguardSecure(true)
            setIsDeviceSecure(true)
        }
        // 모델 허용 목록에 기대지 않고 등록된 지문이 있는 일반 기기를 구성한다.
        ShadowBuild.setModel("generic")
        shadowOf(activity.packageManager).setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true)
        Shadow.extract<ShadowFingerprintManager>(activity.getSystemService("fingerprint")).apply {
            setIsHardwareDetected(true)
            setDefaultFingerprints(1)
        }
        if (Build.VERSION.SDK_INT >= 29) {
            Shadow
                .extract<ShadowBiometricManager>(
                    activity.getSystemService(android.hardware.biometrics.BiometricManager::class.java),
                ).setCanAuthenticate(true)
        }

        // AndroidX 결과를 대역으로 바꾸지 않고 실제 라이브러리 사전 검사를 통과해야 한다.
        assertEquals(BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG))
        if (Build.VERSION.SDK_INT in 28..29) {
            assertEquals(
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
                BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL),
            )
        }
        return activity
    }
}

@AndroidEntryPoint
class BiometricTestActivity : FragmentActivity()

/** 실제 AndroidX 프롬프트가 호출하는 Android framework 경계만 기록한다. */
private object RecordedBiometricSession {
    var authenticationStarts = 0
    lateinit var error: (Int, String) -> Unit
    lateinit var failure: () -> Unit

    fun reset() {
        authenticationStarts = 0
        error = { _, _ -> error("인증이 시작되지 않았다") }
        failure = { error("인증이 시작되지 않았다") }
    }
}

@Implements(value = android.hardware.biometrics.BiometricPrompt::class, minSdk = 28)
class RecordingBiometricPrompt {
    @Implementation
    fun authenticate(
        cancel: CancellationSignal,
        executor: Executor,
        callback: android.hardware.biometrics.BiometricPrompt.AuthenticationCallback,
    ) {
        RecordedBiometricSession.authenticationStarts++
        RecordedBiometricSession.error = { code, message -> executor.execute { callback.onAuthenticationError(code, message) } }
        RecordedBiometricSession.failure = { executor.execute { callback.onAuthenticationFailed() } }
        cancel.setOnCancelListener {
            executor.execute { callback.onAuthenticationError(BiometricPrompt.ERROR_CANCELED, "canceled") }
        }
    }

    @Implementation
    fun authenticate(
        crypto: android.hardware.biometrics.BiometricPrompt.CryptoObject,
        cancel: CancellationSignal,
        executor: Executor,
        callback: android.hardware.biometrics.BiometricPrompt.AuthenticationCallback,
    ) {
        authenticate(cancel, executor, callback)
    }
}

// compileSdk 37은 삭제 예정 framework FingerprintManager를 공개 stub에서 제외한다.
// Robolectric의 API 26~29 framework 경계는 클래스 이름으로 지정한다.
@Implements(className = "android.hardware.fingerprint.FingerprintManager")
class RecordingFingerprintManager : ShadowFingerprintManager() {
    @Implementation
    fun authenticate(
        @ClassName("android.hardware.fingerprint.FingerprintManager\$CryptoObject") crypto: Any?,
        cancel: CancellationSignal?,
        flags: Int,
        @ClassName("android.hardware.fingerprint.FingerprintManager\$AuthenticationCallback") callback: Any,
        handler: Handler?,
    ) {
        RecordedBiometricSession.authenticationStarts++
        RecordedBiometricSession.error = { code, message ->
            ReflectionHelpers.callInstanceMethod<Any?>(
                callback,
                "onAuthenticationError",
                ClassParameter.from(Int::class.javaPrimitiveType, code),
                ClassParameter.from(CharSequence::class.java, message),
            )
        }
        RecordedBiometricSession.failure = {
            ReflectionHelpers.callInstanceMethod<Any?>(callback, "onAuthenticationFailed")
        }
        cancel?.setOnCancelListener { RecordedBiometricSession.error(BiometricPrompt.ERROR_CANCELED, "canceled") }
    }
}
