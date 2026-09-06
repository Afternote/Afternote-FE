package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.UiText
import com.afternote.core.ui.asString
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.setting.presentation.viewmodel.AppLockSetupViewModel
import com.afternote.feature.setting.presentation.viewmodel.PassKeyViewModel
import com.afternote.feature.setting.presentation.viewmodel.PasskeyRegistrationResult
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var isRegistering by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<UiText?>(null) }

    val register: () -> Unit = {
        if (uiState.isComplete && !isRegistering) {
            isRegistering = true
            val pin = uiState.pin
            scope.launch {
                try {
                    when (
                        val result = registerPasskeyWithCredentialManager(context, credentialManager, passKeyViewModel)
                    ) {
                        PasskeyRegistrationResult.Success -> currentOnPinComplete(pin)
                        PasskeyRegistrationResult.Canceled -> Unit
                        is PasskeyRegistrationResult.Error -> errorMessage = result.message
                    }
                } finally {
                    viewModel.resetPin()
                    isRegistering = false
                }
            }
        }
    }
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) register()
    }

    errorMessage?.let { message ->
        Popup(
            type = PopupType.Default,
            message = message.asString(),
            onConfirm = { errorMessage = null },
            onDismiss = { errorMessage = null },
        )
    }

    PassKeyPasswordContent(
        passwordLength = uiState.pin.length,
        onDigitClick = { if (!isRegistering) viewModel.onDigitInput(it) },
        onDeleteClick = { if (!isRegistering) viewModel.onDelete() },
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
