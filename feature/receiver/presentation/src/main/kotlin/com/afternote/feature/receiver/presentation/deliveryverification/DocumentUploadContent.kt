package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.deliveryverification.component.DocumentSlotCard
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_HEADER_SPACING
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_TOTAL_STEPS
import com.afternote.feature.receiver.presentation.deliveryverification.component.ReceiverVerifyStep

@Composable
internal fun DocumentUploadScreenContent(
    uiState: DocumentUploadUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onSlotClick: (DocumentSlot) -> Unit,
    onFamilyFieldBottomChanged: (Int) -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowStepScaffold(
        topBarTitle = stringResource(R.string.receiver_verify_title),
        actionButtonText = stringResource(R.string.receiver_verify_next_button),
        onBackClick = onBackClick,
        onActionClick = onSubmitClick,
        isActionEnabled = uiState.canSubmit,
        currentStep = ReceiverVerifyStep.DOCUMENTS,
        totalSteps = RECEIVER_VERIFY_TOTAL_STEPS,
        progressContentDescription = stringResource(R.string.receiver_verify_step_description, ReceiverVerifyStep.DOCUMENTS),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(RECEIVER_VERIFY_HEADER_SPACING))
        Text(
            text = stringResource(R.string.receiver_verify_document_upload_title),
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.black,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.receiver_verify_document_upload_description),
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray5,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DocumentSlotCard(
                title = stringResource(R.string.receiver_verify_death_cert_title),
                slot = uiState.deathCertificate,
                onPickClick = { onSlotClick(DocumentSlot.DeathCertificate) },
            )
            DocumentSlotCard(
                title = stringResource(R.string.receiver_verify_family_cert_title),
                slot = uiState.familyRelationCertificate,
                onPickClick = { onSlotClick(DocumentSlot.FamilyRelationCertificate) },
                modifier =
                    Modifier.onGloballyPositioned { coords ->
                        onFamilyFieldBottomChanged(coords.boundsInWindow().bottom.toInt())
                    },
            )
        }
    }
}
