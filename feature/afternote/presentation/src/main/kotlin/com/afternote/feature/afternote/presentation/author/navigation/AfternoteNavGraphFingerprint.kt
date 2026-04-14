package com.afternote.feature.afternote.presentation.author.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.fingerprint.BiometricMessages
import com.afternote.feature.afternote.presentation.shared.fingerprint.FingerprintLoginScreen
import com.afternote.feature.afternote.presentation.shared.fingerprint.rememberBiometricAuthenticator

@Composable
internal fun AfternoteFingerprintLoginNavigation(
    onAuthenticationSuccess: () -> Unit,
    onShowError: (String) -> Unit,
) {
    val messages =
        BiometricMessages(
            initFailed = stringResource(R.string.biometric_init_failed),
            noHardware = stringResource(R.string.biometric_no_hardware),
            noneEnrolled = stringResource(R.string.biometric_none_enrolled),
            hwUnavailable = stringResource(R.string.biometric_hw_unavailable),
            notAvailable = stringResource(R.string.biometric_not_available),
        )
    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.biometric_prompt_subtitle)

    val biometricAuthenticator =
        rememberBiometricAuthenticator(
            onAuthSuccess = onAuthenticationSuccess,
            onError = onShowError,
        )

    FingerprintLoginScreen(
        onFingerprintAuthClick = {
            biometricAuthenticator?.authenticate(promptTitle, promptSubtitle, messages)
                ?: onShowError(messages.initFailed)
        },
    )
}
