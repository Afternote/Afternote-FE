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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.InsertPasswordContent
import com.afternote.feature.setting.presentation.viewmodel.AppLockSetupViewModel

@Composable
fun PassKeyPasswordScreen(
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
