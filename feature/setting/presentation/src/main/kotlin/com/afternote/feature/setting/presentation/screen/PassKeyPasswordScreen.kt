package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.setting.presentation.viewmodel.AppLockSetupViewModel
import com.afternote.feature.setting.presentation.viewmodel.PassKeyViewModel

@Composable
fun PassKeyPasswordScreen(
    onPinComplete: (pin: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLockSetupViewModel = hiltViewModel(),
    passKeyViewModel: PassKeyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnPinComplete by rememberUpdatedState(onPinComplete)

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            passKeyViewModel.savePasskeyRegistered()
            currentOnPinComplete(uiState.pin)
            viewModel.resetPin()
        }
    }

    PassKeyPasswordContent(
        passwordLength = uiState.pin.length,
        onDigitClick = viewModel::onDigitInput,
        onDeleteClick = viewModel::onDelete,
        onConfirmClick = { currentOnPinComplete(uiState.pin) },
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
