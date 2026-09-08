package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.setting.presentation.component.PinSetupStep
import com.afternote.feature.setting.presentation.viewmodel.AppLockSetupViewModel

@Composable
fun AppLockSetupScreen(
    step: PinSetupStep,
    onPinComplete: (pin: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLockSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnPinComplete by rememberUpdatedState(onPinComplete)

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            currentOnPinComplete(uiState.pin)
            viewModel.resetPin()
        }
    }

    AppLockSetupContent(
        step = step,
        passwordLength = uiState.pin.length,
        onDigitClick = viewModel::onDigitInput,
        onDeleteClick = viewModel::onDelete,
        onConfirmClick = { currentOnPinComplete(uiState.pin) },
        onBack = onBack,
        modifier = modifier,
    )
}
