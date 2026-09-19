package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.AfternoteTextField
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_HEADER_SPACING
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_TOTAL_STEPS
import com.afternote.feature.receiver.presentation.deliveryverification.component.ReceiverVerifyStep

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
