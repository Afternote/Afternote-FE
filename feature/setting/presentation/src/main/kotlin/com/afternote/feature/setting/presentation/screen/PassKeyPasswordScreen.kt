package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.setting.presentation.viewmodel.AppLockSetupIntent
import com.afternote.feature.setting.presentation.viewmodel.AppLockSetupViewModel
import com.afternote.feature.setting.presentation.viewmodel.PassKeyIntent
import com.afternote.feature.setting.presentation.viewmodel.PassKeyViewModel
import com.afternote.feature.setting.presentation.viewmodel.PasskeyRegistrationResult

@Composable
internal fun PassKeyPasswordScreen(
    onPinComplete: (pin: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLockSetupViewModel = hiltViewModel(),
    passKeyViewModel: PassKeyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnPinComplete by rememberUpdatedState(onPinComplete)
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val registrationState by passKeyViewModel.uiState.collectAsStateWithLifecycle()
    val isRegistering = registrationState.isRegistering
    DisposableEffect(passKeyViewModel, viewModel) {
        onDispose {
            passKeyViewModel.onIntent(PassKeyIntent.CancelRegistration)
            viewModel.onIntent(AppLockSetupIntent.ResetPin)
        }
    }
    val result = registrationState.result
    ObserveSignal(
        signal = result.takeUnless { it is PasskeyRegistrationResult.Error },
        consumed = PassKeyIntent.ConsumeResult(result ?: PasskeyRegistrationResult.Canceled),
        onIntent = passKeyViewModel::onIntent,
    ) { completed ->
        val pin = uiState.pin
        viewModel.onIntent(AppLockSetupIntent.ResetPin)
        if (completed == PasskeyRegistrationResult.Success) currentOnPinComplete(pin)
    }
    LaunchedEffect(result) {
        if (result is PasskeyRegistrationResult.Error) viewModel.onIntent(AppLockSetupIntent.ResetPin)
    }
    val register: () -> Unit = {
        if (uiState.isComplete && !isRegistering && result == null) {
            passKeyViewModel.onIntent(
                PassKeyIntent.Register { options ->
                    createPasskeyCredential(context, credentialManager, options)
                },
            )
        }
    }
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) register()
    }

    (result as? PasskeyRegistrationResult.Error)?.let { failure ->
        Popup(
            type = PopupType.Default,
            message = failure.message.asString(),
            onConfirm = { passKeyViewModel.onIntent(PassKeyIntent.ConsumeResult(failure)) },
            onDismiss = { passKeyViewModel.onIntent(PassKeyIntent.ConsumeResult(failure)) },
        )
    }

    PassKeyPasswordContent(
        passwordLength = uiState.pin.length,
        onDigitClick = { if (!isRegistering) viewModel.onIntent(AppLockSetupIntent.DigitInput(it)) },
        onDeleteClick = { if (!isRegistering) viewModel.onIntent(AppLockSetupIntent.Delete) },
        onConfirmClick = register,
        onBack = onBack,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun PassKeyPasswordScreenPreview() {
    PassKeyPasswordScreen(
        onPinComplete = {},
        onBack = {},
    )
}
