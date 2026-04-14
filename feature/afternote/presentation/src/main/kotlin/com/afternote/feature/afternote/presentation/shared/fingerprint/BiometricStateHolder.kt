package com.afternote.feature.afternote.presentation.shared.fingerprint

import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.afternote.core.ui.findActivity

private const val LOG_TAG = "FingerprintLogin"

data class BiometricMessages(
    val initFailed: String,
    val noHardware: String,
    val noneEnrolled: String,
    val hwUnavailable: String,
    val notAvailable: String,
)

class BiometricStateHolder(
    private val activity: FragmentActivity,
    private val onAuthSuccess: () -> Unit,
    private val onError: (String) -> Unit,
) {
    private var biometricPrompt: BiometricPrompt? = null

    fun authenticate(
        title: String,
        subtitle: String,
        messages: BiometricMessages,
    ) {
        val biometricManager = BiometricManager.from(activity)
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

                biometricPrompt?.cancelAuthentication()
                biometricPrompt = null

                try {
                    biometricPrompt =
                        BiometricPrompt(
                            activity,
                            ContextCompat.getMainExecutor(activity),
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                    onAuthSuccess()
                                }

                                override fun onAuthenticationError(
                                    errorCode: Int,
                                    errString: CharSequence,
                                ) {
                                    Log.w(LOG_TAG, "Auth Error [$errorCode]: $errString")
                                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                    ) {
                                        onError(errString.toString())
                                    }
                                }

                                override fun onAuthenticationFailed() {
                                    Log.d(LOG_TAG, "Auth Failed: Not recognized")
                                }
                            },
                        ).apply {
                            authenticate(promptInfo)
                        }
                } catch (e: Throwable) {
                    Log.e(LOG_TAG, "BiometricPrompt init failed", e)
                    onError(messages.initFailed)
                }
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onError(messages.noHardware)
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError(messages.noneEnrolled)
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onError(messages.hwUnavailable)
            }

            else -> {
                onError(messages.notAvailable)
            }
        }
    }

    fun cancel() {
        biometricPrompt?.cancelAuthentication()
        biometricPrompt = null
    }
}

@Composable
fun rememberBiometricAuthenticator(
    onAuthSuccess: () -> Unit,
    onError: (String) -> Unit,
): BiometricStateHolder? {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity<FragmentActivity>() }

    val currentOnSuccess by rememberUpdatedState(onAuthSuccess)
    val currentOnError by rememberUpdatedState(onError)

    val stateHolder =
        remember(activity) {
            activity?.let {
                BiometricStateHolder(
                    activity = it,
                    onAuthSuccess = { currentOnSuccess() },
                    onError = { err -> currentOnError(err) },
                )
            }
        }

    DisposableEffect(stateHolder) {
        onDispose {
            stateHolder?.cancel()
        }
    }

    return stateHolder
}
