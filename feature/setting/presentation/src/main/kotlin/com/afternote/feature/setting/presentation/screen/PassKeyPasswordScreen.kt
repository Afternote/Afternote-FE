package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.InsertPasswordContent
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
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            when (
                val registration =
                    registerPasskeyWithCredentialManager(
                        context = context,
                        credentialManager = credentialManager,
                        viewModel = passKeyViewModel,
                    )
            ) {
                PasskeyRegistrationResult.Success -> currentOnPinComplete(uiState.pin)
                PasskeyRegistrationResult.Canceled -> Unit
                is PasskeyRegistrationResult.Error -> errorMessage = registration.message
            }
            viewModel.resetPin()
        }
    }

    errorMessage?.let { msg ->
        Popup(
            type = PopupType.Default,
            message = msg,
            onConfirm = { errorMessage = null },
            onDismiss = { errorMessage = null },
        )
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(id = R.string.passkey_management_title),
                onBackClick = onBack,
            )
        },
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            InsertPasswordContent(
                titleText = "비밀번호를 입력해 주세요.",
                passwordLength = uiState.pin.length,
                onDigitClick = viewModel::onDigitInput,
                onDeleteClick = viewModel::onDelete,
                onConfirmClick = { currentOnPinComplete(uiState.pin) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PassKeyPasswordScreenPreview() {
    PassKeyPasswordScreen(
        onPinComplete = {},
        onBack = {},
    )
}
