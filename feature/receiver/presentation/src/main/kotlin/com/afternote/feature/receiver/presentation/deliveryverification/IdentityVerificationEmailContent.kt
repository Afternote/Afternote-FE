package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.TextFieldType
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_HEADER_SPACING
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_TOTAL_STEPS
import com.afternote.feature.receiver.presentation.deliveryverification.component.ReceiverVerifyStep

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
