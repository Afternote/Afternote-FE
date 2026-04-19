package com.afternote.feature.afternote.presentation.shared.fingerprint

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

private const val LOG_TAG = "FingerprintLogin"

/**
 * 생체 인식 프롬프트에 노출할 상황별 문구.
 * `stringResource`로 해석된 값이 필요하므로 Composable에서 빌드해 주입한다.
 */
data class BiometricMessages(
    val initFailed: String,
    val noHardware: String,
    val noneEnrolled: String,
    val hwUnavailable: String,
    val notAvailable: String,
)

/**
 * 생체 인식 결과.
 *
 * UI 레이어에서 단방향으로 흘려 성공·취소·오류를 명시적으로 분기한다.
 * 취소는 오류로 노출하지 않도록 별도 타입으로 정의한다.
 */
sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult

    data object Canceled : BiometricAuthResult

    data class Error(
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
 */
suspend fun FragmentActivity.authenticateBiometric(
    title: String,
    subtitle: String,
    messages: BiometricMessages,
): BiometricAuthResult =
    withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            val biometricManager = BiometricManager.from(this@authenticateBiometric)
            val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

            when (biometricManager.canAuthenticate(authenticators)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    val promptInfo =
                        BiometricPrompt.PromptInfo
                            .Builder()
                            .setTitle(title)
                            .setSubtitle(subtitle)
                            .setAllowedAuthenticators(authenticators)
                            .build()

                    val biometricPrompt =
                        BiometricPrompt(
                            this@authenticateBiometric,
                            ContextCompat.getMainExecutor(this@authenticateBiometric),
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    if (continuation.isActive) {
                                        continuation.resume(BiometricAuthResult.Success)
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
                        biometricPrompt.authenticate(promptInfo)
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
