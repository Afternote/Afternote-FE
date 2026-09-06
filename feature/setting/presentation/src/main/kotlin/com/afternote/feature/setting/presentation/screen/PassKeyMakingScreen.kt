package com.afternote.feature.setting.presentation.screen

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.afternote.core.ui.UiText
import com.afternote.core.ui.asString
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.PassKeyViewModel
import com.afternote.feature.setting.presentation.viewmodel.PasskeyRegistrationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private sealed interface BiometricResult {
    data object Success : BiometricResult

    data object Canceled : BiometricResult

    data class Error(
        val message: String,
    ) : BiometricResult
}

@Composable
internal fun PassKeyMakingScreen(
    onBackClick: () -> Unit,
    onPasswordAuthClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PassKeyViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity<FragmentActivity>() }
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val isBiometricAvailable =
        remember {
            BiometricManager
                .from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        }
    var showCompletionDialog by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<UiText?>(null) }

    if (showCompletionDialog) {
        Popup(
            type = PopupType.Default,
            message = stringResource(R.string.setting_passkey_registration_complete),
            onConfirm = {
                showCompletionDialog = false
                onBackClick()
            },
            onDismiss = { showCompletionDialog = false },
        )
    }

    errorMessage?.let { msg ->
        Popup(
            type = PopupType.Default,
            message = msg.asString(),
            onConfirm = { errorMessage = null },
            onDismiss = { errorMessage = null },
        )
    }

    PassKeyMakingContent(
        onBackClick = onBackClick,
        onBiometricAuthClick = {
            if (!isAuthenticating && activity != null) {
                isAuthenticating = true
                coroutineScope.launch {
                    try {
                        when (val result = authenticate(activity)) {
                            BiometricResult.Success -> {
                                when (
                                    val registration =
                                        registerPasskeyWithCredentialManager(activity, credentialManager, viewModel)
                                ) {
                                    PasskeyRegistrationResult.Success -> showCompletionDialog = true
                                    PasskeyRegistrationResult.Canceled -> Unit
                                    is PasskeyRegistrationResult.Error -> errorMessage = registration.message
                                }
                            }

                            BiometricResult.Canceled -> {
                                Unit
                            }

                            is BiometricResult.Error -> {
                                errorMessage = UiText.Dynamic(result.message)
                            }
                        }
                    } finally {
                        isAuthenticating = false
                    }
                }
            }
        },
        onPasswordAuthClick = onPasswordAuthClick,
        isBiometricAvailable = isBiometricAvailable,
        modifier = modifier,
    )
}

private suspend fun authenticate(activity: FragmentActivity): BiometricResult =
    suspendCancellableCoroutine { continuation ->
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle("패스키 등록")
                .setSubtitle("패스키로 사용할 지문을 인식해 주세요.")
                .setAllowedAuthenticators(authenticators)
                .build()

        val biometricPrompt =
            BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(BiometricResult.Success)
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        if (!continuation.isActive) return
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        ) {
                            continuation.resume(BiometricResult.Canceled)
                        } else {
                            continuation.resume(BiometricResult.Error(errString.toString()))
                        }
                    }

                    override fun onAuthenticationFailed() {}
                },
            )

        continuation.invokeOnCancellation { biometricPrompt.cancelAuthentication() }
        biometricPrompt.authenticate(promptInfo)
    }

@Preview(showBackground = true)
@Composable
private fun PassKeyMakingScreenPrev() {
    PassKeyMakingScreen(onBackClick = {}, onPasswordAuthClick = {})
}
