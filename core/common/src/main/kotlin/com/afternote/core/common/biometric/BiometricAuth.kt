package com.afternote.core.common.biometric

import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private const val LOG_TAG = "BiometricAuth"

/**
 * 생체 인식 프롬프트에 노출할 상황별 문구.
 * `stringResource`로 해석된 값이 필요하므로 Composable에서 빌드해 주입한다.
 * 문구는 소비 feature 마다 다르므로 core 는 값을 정하지 않고 호출 측이 넘긴다.
 */
public data class BiometricMessages(
    val initFailed: String,
    val noHardware: String,
    val noneEnrolled: String,
    val hwUnavailable: String,
    val notAvailable: String,
    val verificationFailed: String,
)

/**
 * 생체 인식 결과.
 *
 * UI 레이어에서 단방향으로 흘려 성공·취소·오류를 명시적으로 분기한다.
 * 취소는 오류로 노출하지 않도록 별도 타입으로 정의한다.
 */
public sealed interface BiometricAuthResult {
    public data object Success : BiometricAuthResult

    public data object Canceled : BiometricAuthResult

    public data class Error(
        val message: String,
    ) : BiometricAuthResult
}

/**
 * [BiometricPrompt]의 콜백 API를 [suspendCancellableCoroutine]으로 감싸 코루틴 흐름으로 변환한다.
 *
 * - 호출 측 코루틴이 취소되면 [BiometricPrompt.cancelAuthentication]이 자동 호출되어
 *   화면 이동 시 프롬프트 누수가 방지된다 (기존 `DisposableEffect` 대체).
 * - 사용자가 취소/음수 버튼을 누르면 [BiometricAuthResult.Canceled]를 반환해
 *   호출 측에서 오류 UI를 띄우지 않도록 한다.
 * - `androidx.biometric` SDK가 [FragmentActivity]를 요구하므로 이 함수는 해당 타입의 확장으로 정의한다.
 *   `MainActivity`는 반드시 `FragmentActivity`(또는 `AppCompatActivity`)를 상속해야 한다.
 * - [BiometricPrompt]는 UI 컴포넌트이므로 본문을 [Dispatchers.Main.immediate]에서 실행해,
 *   호출 코루틴이 백그라운드 디스패처에 있어도 초기화·실행이 메인 스레드로 수렴하도록 한다.
 * - 성공 콜백의 cipher는 [confirmWithCryptoOperation]으로 연산을 검증한다.
 *   API 30 이상에서 앱 소유 CryptoObject를 준비한 경우에는 사용자 인증에 묶인 키이므로
 *   인증 없이 성공 콜백만 가로채 호출하면 연산이 실패한다. AndroidX의 내부 cipher에는
 *   이 인증 결합을 가정하지 않으며, 플랫폼 프롬프트의 강한 생체 인증 결과를 따른다.
 */
public suspend fun FragmentActivity.authenticateBiometric(
    title: String,
    subtitle: String,
    messages: BiometricMessages,
): BiometricAuthResult =
    withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val biometricManager = BiometricManager.from(this@authenticateBiometric)
            // Android 9·10은 STRONG과 기기 자격의 조합을 지원하지 않는다.
            // 강도를 낮추지 않고 이 두 버전에서만 생체 인증을 단독 사용한다.
            val biometricOnly = Build.VERSION.SDK_INT in Build.VERSION_CODES.P..Build.VERSION_CODES.Q
            val authenticators = if (biometricOnly) BIOMETRIC_STRONG else BIOMETRIC_STRONG or DEVICE_CREDENTIAL

            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    // 앱 소유 CryptoObject는 기존 API 30 이상 정책을 유지한다.
                    // API 28~29의 STRONG 강제는 AndroidX 프롬프트가 담당한다.
                    val cryptoObject = if (isBiometricCryptoSupported) createBiometricCryptoObject() else null

                    val promptInfo =
                        BiometricPrompt.PromptInfo
                            .Builder()
                            .setTitle(title)
                            .setSubtitle(subtitle)
                            .setAllowedAuthenticators(authenticators)
                            .apply {
                                if (biometricOnly) {
                                    setNegativeButtonText(getString(android.R.string.cancel))
                                }
                            }.build()

                    val biometricPrompt =
                        BiometricPrompt(
                            this@authenticateBiometric,
                            ContextCompat.getMainExecutor(this@authenticateBiometric),
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    if (!continuation.isActive) return
                                    val confirmation = confirmWithCryptoOperation(result.cryptoObject?.cipher)
                                    if (confirmation.isSuccess) {
                                        continuation.resume(BiometricAuthResult.Success)
                                    } else {
                                        Log.w(LOG_TAG, "인증 후 암호 연산 실패", confirmation.exceptionOrNull())
                                        continuation.resume(BiometricAuthResult.Error(messages.verificationFailed))
                                    }
                                }

                                override fun onAuthenticationError(
                                    errorCode: Int,
                                    errString: CharSequence,
                                ) {
                                    if (!continuation.isActive) return
                                    // 사용자 취소(USER_CANCELED/NEGATIVE_BUTTON) 외에 시스템이 프롬프트를 닫는
                                    // ERROR_CANCELED(앱 전환 등) 까지 Canceled 로 래핑해 불필요한 에러 UI 노출을 막는다.
                                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                        errorCode == BiometricPrompt.ERROR_CANCELED
                                    ) {
                                        continuation.resume(BiometricAuthResult.Canceled)
                                    } else {
                                        Log.w(LOG_TAG, "Auth Error [$errorCode]: $errString")
                                        continuation.resume(BiometricAuthResult.Error(errString.toString()))
                                    }
                                }

                                override fun onAuthenticationFailed() {
                                    // 지문 불일치: 프롬프트 유지 및 사용자 재시도 유도를 위해 resume 하지 않는다.
                                    Log.d(LOG_TAG, "Auth Failed: Not recognized")
                                }
                            },
                        )

                    continuation.invokeOnCancellation {
                        biometricPrompt.cancelAuthentication()
                    }

                    try {
                        if (cryptoObject != null) {
                            biometricPrompt.authenticate(promptInfo, cryptoObject)
                        } else {
                            biometricPrompt.authenticate(promptInfo)
                        }
                    } catch (e: Throwable) {
                        Log.e(LOG_TAG, "BiometricPrompt init failed", e)
                        if (continuation.isActive) {
                            continuation.resume(BiometricAuthResult.Error(messages.initFailed))
                        }
                    }
                }

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    continuation.resume(BiometricAuthResult.Error(messages.noHardware))
                }

                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    continuation.resume(BiometricAuthResult.Error(messages.noneEnrolled))
                }

                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    continuation.resume(BiometricAuthResult.Error(messages.hwUnavailable))
                }

                else -> {
                    continuation.resume(BiometricAuthResult.Error(messages.notAvailable))
                }
            }
        }
    }
