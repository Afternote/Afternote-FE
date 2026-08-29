package com.afternote.feature.receiver.presentation.deliveryverification

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.receiver.presentation.R
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_HEADER_SPACING
import com.afternote.feature.receiver.presentation.deliveryverification.component.RECEIVER_VERIFY_TOTAL_STEPS
import com.afternote.feature.receiver.presentation.deliveryverification.component.ReceiverVerifyStep

/**
 * 열람 신청 완료 화면(design 9) — `submitDeliveryVerification` 성공 직후 노출 (이슈 #215).
 *
 * 시안 그대로: TopBar "수신자 인증" + h1 "열람 신청 완료" + 안내 2 줄 + 하단 CTA "받은 기록함으로 돌아가기".
 * 진행 인디케이터는 마지막 단계(4/4)로 100% 채워 표시 — 열람 신청 완료를 꽉 찬 바로 나타낸다.
 * 실제 신청 제출은 [DocumentUploadViewModel.submit] 이 수행하므로 본 화면은 결과 안내 + 복귀 액션만 제공한다.
 */
@Composable
fun DeliveryVerificationCompleteScreen(
    onBackToRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowStepScaffold(
        topBarTitle = stringResource(R.string.receiver_verify_title),
        actionButtonText = stringResource(R.string.receiver_verify_complete_back_to_records),
        onBackClick = onBackToRecords,
        onActionClick = onBackToRecords,
        currentStep = ReceiverVerifyStep.COMPLETE,
        totalSteps = RECEIVER_VERIFY_TOTAL_STEPS,
        progressContentDescription = stringResource(R.string.receiver_verify_step_description, ReceiverVerifyStep.COMPLETE),
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(RECEIVER_VERIFY_HEADER_SPACING))
        Text(
            text = stringResource(R.string.receiver_verify_complete_title),
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.black,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.receiver_verify_complete_description),
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray5,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
