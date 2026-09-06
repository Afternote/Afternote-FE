package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.asString
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_HEADER_SPACING
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_TOTAL_STEPS
import com.afternote.feature.receiver.presentation.deliveryverification.component.ReceiverVerifyStep

/**
 * 마스터 키 입력 화면(design 5) — 진행 인디케이터 2/3 + 단일 입력 + "다음" CTA (이슈 #215).
 *
 * `verify(masterKey)` 성공 시 SenderRegistry 에 masterKey·신원 결합 + 글로벌 헤더 저장 → [onVerified] 로 서류 업로드 단계 진입.
 *
 * 메모리 정책상 ViewModel 은 TextFieldState 미보유. UI 가 `rememberTextFieldState` 로 인스턴스를 들고 있다가 submit 시점에만 값을 전달.
 */
@Composable
fun MasterKeyScreen(
    senderId: String,
    onBackClick: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MasterKeyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val masterKeyState = rememberTextFieldState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerified()
            viewModel.onVerifiedConsumed()
        }
    }

    val errorMessage =
        uiState.errorMessage?.asString()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.consumeError()
        }
    }

    MasterKeyScreenContent(
        masterKeyState = masterKeyState,
        isSubmitting = uiState.isSubmitting,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onSubmitClick = { viewModel.submit(senderId, masterKeyState.text.toString()) },
        modifier = modifier,
    )
}

@Composable
internal fun MasterKeyScreenContent(
    masterKeyState: TextFieldState,
    isSubmitting: Boolean,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInputFilled =
        masterKeyState.text
            .toString()
            .trim()
            .isNotEmpty()
    val canSubmit = isInputFilled && !isSubmitting

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.receiver_verify_title),
        actionButtonText = stringResource(R.string.receiver_verify_next_button),
        onBackClick = onBackClick,
        onActionClick = onSubmitClick,
        isActionEnabled = canSubmit,
        currentStep = ReceiverVerifyStep.MASTER_KEY,
        totalSteps = RECEIVER_VERIFY_TOTAL_STEPS,
        progressContentDescription = stringResource(R.string.receiver_verify_step_description, ReceiverVerifyStep.MASTER_KEY),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(RECEIVER_VERIFY_HEADER_SPACING))
        Text(
            text = stringResource(R.string.receiver_verify_master_key_title),
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.black,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.receiver_verify_master_key_description),
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray5,
        )

        Spacer(modifier = Modifier.height(16.dp))

        AfternoteTextField(
            state = masterKeyState,
            placeholder = stringResource(R.string.receiver_verify_master_key_placeholder),
            modifier = Modifier.fillMaxWidth().imePadding(),
        )
    }
}
