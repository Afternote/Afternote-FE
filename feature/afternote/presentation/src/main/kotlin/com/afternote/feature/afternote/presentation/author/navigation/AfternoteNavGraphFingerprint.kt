package com.afternote.feature.afternote.presentation.author.navigation

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.fingerprint.FingerprintLoginScreen

@Composable
internal fun AfternoteFingerprintLoginNavigation(onAuthenticationSuccess: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val onAuthSuccessState = rememberUpdatedState(onAuthenticationSuccess)

    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.biometric_prompt_subtitle)
    val notAvailableMessage = stringResource(R.string.biometric_not_available)
    val initFailedMessage = stringResource(R.string.biometric_init_failed)
    val noHardwareMessage = stringResource(R.string.biometric_no_hardware)
    val noneEnrolledMessage = stringResource(R.string.biometric_none_enrolled)
    val hwUnavailableMessage = stringResource(R.string.biometric_hw_unavailable)

    val biometricPrompt =
        remember(activity) {
            if (activity == null) return@remember null
            try {
                val executor = ContextCompat.getMainExecutor(activity)
                BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onAuthSuccessState.value()
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            Log.w(TAG_FINGERPRINT, "Auth Error [$errorCode]: $errString")
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                            ) {
                                Toast
                                    .makeText(context, errString, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Log.d(TAG_FINGERPRINT, "Auth Failed: Not recognized")
                        }
                    },
                )
            } catch (e: Throwable) {
                Log.e(TAG_FINGERPRINT, "BiometricPrompt init failed", e)
                null
            }
        }

    val promptInfo =
        remember(promptTitle, promptSubtitle) {
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(promptTitle)
                .setSubtitle(promptSubtitle)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
        }

    FingerprintLoginScreen(
        onFingerprintAuthClick = {
            if (activity == null || biometricPrompt == null) {
                Log.e(TAG_FINGERPRINT, "Auth clicked but activity or BiometricPrompt is null")
                Toast
                    .makeText(context, initFailedMessage, Toast.LENGTH_SHORT)
                    .show()
                return@FingerprintLoginScreen
            }

            val biometricManager = BiometricManager.from(context)
            when (
                val authStatus =
                    biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            ) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    Log.d(TAG_FINGERPRINT, "canAuthenticate: SUCCESS, launching prompt")
                    biometricPrompt.authenticate(promptInfo)
                }

                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    Log.e(TAG_FINGERPRINT, "canAuthenticate: NO_HARDWARE")
                    Toast
                        .makeText(context, noHardwareMessage, Toast.LENGTH_LONG)
                        .show()
                }

                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Log.e(TAG_FINGERPRINT, "canAuthenticate: NONE_ENROLLED")
                    Toast
                        .makeText(context, noneEnrolledMessage, Toast.LENGTH_LONG)
                        .show()
                }

                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    Log.e(TAG_FINGERPRINT, "canAuthenticate: HW_UNAVAILABLE")
                    Toast
                        .makeText(context, hwUnavailableMessage, Toast.LENGTH_SHORT)
                        .show()
                }

                else -> {
                    Log.e(TAG_FINGERPRINT, "canAuthenticate: code=$authStatus")
                    Toast
                        .makeText(context, notAvailableMessage, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        },
    )
}

/**
 * [ContextWrapper] 체인을 따라가 [FragmentActivity]를 찾는다.
 * [LocalContext]가 래핑된 경우에도 생체 프롬프트에 필요한 Activity를 얻기 위함이다.
 */
private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
    when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
