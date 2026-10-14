package com.afternote.feature.setting.presentation.passkey

import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.common.biometric.BiometricAuthResult
import com.afternote.core.common.biometric.BiometricMessages
import com.afternote.core.common.biometric.authenticateBiometric
import com.afternote.core.ui.asString
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.launch

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
    val isBiometricAvailable =
        remember {
            BiometricManager
                .from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val registrationState by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        onDispose { viewModel.onIntent(PassKeyIntent.CancelRegistration) }
    }
    ObserveSignal(
        signal = registrationState.registrationId.takeIf { registrationState.result == PasskeyRegistrationResult.Canceled },
        consumed = PassKeyIntent.ConsumeResult(registrationState.registrationId, PasskeyRegistrationResult.Canceled),
        onIntent = viewModel::onIntent,
    ) { }
    val consumeResult: () -> Unit = {
        registrationState.result?.let {
            viewModel.onIntent(PassKeyIntent.ConsumeResult(registrationState.registrationId, it))
        }
    }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val messages =
        BiometricMessages(
            initFailed = stringResource(R.string.setting_biometric_init_failed),
            noHardware = stringResource(R.string.setting_biometric_no_hardware),
            noneEnrolled = stringResource(R.string.setting_biometric_none_enrolled),
            hwUnavailable = stringResource(R.string.setting_biometric_hw_unavailable),
            notAvailable = stringResource(R.string.setting_biometric_not_available),
            verificationFailed = stringResource(R.string.setting_biometric_verification_failed),
        )
    val promptTitle = stringResource(R.string.setting_biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.setting_biometric_prompt_subtitle)

    if (registrationState.result == PasskeyRegistrationResult.Success) {
        Popup(
            type = PopupType.Default,
            message = stringResource(R.string.setting_passkey_registration_complete),
            onConfirm = {
                consumeResult()
                onBackClick()
            },
            onDismiss = { consumeResult() },
        )
    }

    (registrationState.result as? PasskeyRegistrationResult.Error)?.let { failure ->
        Popup(
            type = PopupType.Default,
            message = failure.message.asString(),
            onConfirm = consumeResult,
            onDismiss = consumeResult,
        )
    }

    errorMessage?.let { msg ->
        Popup(
            type = PopupType.Default,
            message = msg,
            onConfirm = { errorMessage = null },
            onDismiss = { errorMessage = null },
        )
    }

    PassKeyMakingContent(
        onBackClick = onBackClick,
        onBiometricAuthClick = {
            if (!isAuthenticating && !registrationState.isRegistering && registrationState.result == null && activity != null) {
                isAuthenticating = true
                coroutineScope.launch {
                    try {
                        when (val result = activity.authenticateBiometric(promptTitle, promptSubtitle, messages)) {
                            BiometricAuthResult.Success -> {
                                viewModel.onIntent(
                                    PassKeyIntent.Register { options ->
                                        createPasskeyCredential(activity, credentialManager, options)
                                    },
                                )
                            }

                            BiometricAuthResult.Canceled -> {
                                Unit
                            }

                            is BiometricAuthResult.Error -> {
                                errorMessage = result.message
                            }
                        }
                    } finally {
                        isAuthenticating = false
                    }
                }
            }
        },
        onPasswordAuthClick = { if (!isAuthenticating && !registrationState.isRegistering) onPasswordAuthClick() },
        isBiometricAvailable = isBiometricAvailable,
        modifier = modifier,
    )
}
