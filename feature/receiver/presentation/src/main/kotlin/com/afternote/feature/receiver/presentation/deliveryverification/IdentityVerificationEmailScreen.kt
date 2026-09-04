package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.asString
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_HEADER_SPACING
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_TOTAL_STEPS
import com.afternote.feature.receiver.presentation.deliveryverification.component.ReceiverVerifyStep
import com.afternote.feature.receiver.presentation.error.ReceiverErrorPopupHost

/**
 * 본인 확인 이메일 인증(designs 3·4) — 이메일 + 인증번호 입력 화면 (이슈 #215).
 *
 * 디자인 3 (입력 전) 과 4 (인증번호 발송 후 안내 메시지 표시) 는 동일 화면. 발송 직후 두 입력 필드 아래
 * 강조 안내 텍스트가 나타난다.
 *
 * 인증번호 발송·검증은 실 API(`receiver-auth/email` 계열) — 서버 거절 안내 문구(이메일 미등록 등) 는
 * 스낵바로 노출된다 (#407). 서버·네트워크 실패는 스낵바가 아니라 공통 오류 팝업이 맡는다 (#446) —
 * 사용자가 할 일이 재시도뿐이라 스스로 사라지는 안내로는 그 액션을 줄 자리가 없다.
 *
 * `senderId` 는 [MasterKeyScreen] 과 같은 규약으로 parent backStackEntry 의
 * [DeliveryVerificationFlowViewModel] 에서 받아 검증 성공 시점의 발신자별 캐시 기록에 쓴다 (#597).
 */
@Composable
fun IdentityVerificationEmailScreen(
    senderId: String,
    onBackClick: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IdentityVerificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emailState = rememberTextFieldState()
    val codeState = rememberTextFieldState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(emailState) {
        snapshotFlow { emailState.text.toString() }.collect(viewModel::onEmailChange)
    }
    LaunchedEffect(codeState) {
        snapshotFlow { codeState.text.toString() }.collect(viewModel::onCodeChange)
    }

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

    IdentityVerificationEmailScreenContent(
        uiState = uiState,
        emailState = emailState,
        codeState = codeState,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onRequestCode = viewModel::requestVerificationCode,
        onVerifyAndProceed = { viewModel.verifyAndProceed(senderId) },
        modifier = modifier,
    )

    ReceiverErrorPopupHost(
        popup = uiState.errorPopup,
        onRetry = viewModel::retryFailedRequest,
        onDismiss = viewModel::onErrorPopupDismissed,
    )
}

@Composable
internal fun IdentityVerificationEmailScreenContent(
    uiState: IdentityVerificationUiState,
    emailState: TextFieldState,
    codeState: TextFieldState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onRequestCode: () -> Unit,
    onVerifyAndProceed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requestCodeText =
        if (uiState.isSendingCode) {
            stringResource(R.string.receiver_verify_code_requesting)
        } else {
            stringResource(R.string.receiver_verify_request_code)
        }

    FlowStepScaffold(
        topBarTitle = stringResource(R.string.receiver_verify_title),
        actionButtonText = stringResource(R.string.receiver_verify_next_button),
        onBackClick = onBackClick,
        onActionClick = onVerifyAndProceed,
        isActionEnabled = uiState.canSubmit,
        currentStep = ReceiverVerifyStep.IDENTITY,
        totalSteps = RECEIVER_VERIFY_TOTAL_STEPS,
        progressContentDescription = stringResource(R.string.receiver_verify_step_description, ReceiverVerifyStep.IDENTITY),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(RECEIVER_VERIFY_HEADER_SPACING))
        Column(modifier = Modifier.imePadding()) {
            Text(
                text = stringResource(R.string.receiver_verify_self_title),
                style = AfternoteDesign.typography.h1,
                color = AfternoteDesign.colors.black,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.receiver_verify_email_description),
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.gray5,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AfternoteTextField(
                    state = emailState,
                    type =
                        TextFieldType.Variant7(
                            text = requestCodeText,
                            onClick = onRequestCode,
                            enabled = uiState.isEmailFormatValid && !uiState.isSendingCode,
                        ),
                    placeholder = stringResource(R.string.receiver_verify_email_placeholder),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                )

                if (uiState.email.isNotBlank() && !uiState.isEmailFormatValid) {
                    Text(
                        text = stringResource(R.string.receiver_verify_email_format_invalid),
                        style = AfternoteDesign.typography.captionLargeB,
                        color = AfternoteDesign.colors.b1,
                    )
                }

                AfternoteTextField(
                    state = codeState,
                    placeholder = stringResource(R.string.receiver_verify_code_placeholder),
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (uiState.canSubmit) onVerifyAndProceed()
                    },
                )

                if (uiState.isVerificationSent) {
                    Text(
                        text = stringResource(R.string.receiver_verify_code_sent),
                        style = AfternoteDesign.typography.captionLargeB,
                        color = AfternoteDesign.colors.b1,
                    )
                }
            }
        }
    }
}
