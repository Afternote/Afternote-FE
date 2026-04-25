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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.component.InsertPasswordContent
import com.afternote.feature.setting.presentation.component.PinSetupStep
import com.afternote.feature.setting.presentation.viewmodel.AppLockSetupViewModel

@Composable
fun AppLockSetupScreen(
    step: PinSetupStep,
    onPinComplete: (pin: String) -> Unit,
    onBack: () -> Unit,
    viewModel: AppLockSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onPinComplete(uiState.pin)
            viewModel.resetPin()
        }
    }

    val titleText =
        when (step) {
            PinSetupStep.ENTER_NEW -> "비밀번호를 입력해 주세요."
            PinSetupStep.CONFIRM_NEW -> "비밀번호를 다시 한 번 입력해 주세요."
            PinSetupStep.ENTER_CURRENT -> "변경할 비밀번호를 입력해 주세요."
        }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = "앱 잠금 비밀번호 설정",
                onBackClick = onBack,
            )
        },
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
                titleText = titleText,
                passwordLength = uiState.pin.length,
                onDigitClick = viewModel::onDigitInput,
                onDeleteClick = viewModel::onDelete,
            )
        }
    }
}
