package com.afternote.feature.afternote.presentation.receiver.deliveryverification

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.scaffold.FlowStepScaffold
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.component.RECEIVER_VERIFY_HEADER_SPACING
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.component.RECEIVER_VERIFY_TOTAL_STEPS
import com.afternote.feature.afternote.presentation.receiver.deliveryverification.component.ReceiverVerifyStep

/**
 * 수신자 본인 확인 안내(design 2) — 진행 인디케이터 1/3 + 안내 문구 + "인증 시작하기" CTA.
 *
 * 발신자 상세의 "열람 신청하기" 진입 시 [IdentityVerificationGate.isVerified] 가 false 인 경우만 노출된다.
 * 백엔드 `receiver-auth/email/` 미구현 단계라 [IdentityEmailVerificationStub] 으로 시뮬레이션.
 */
@Composable
fun IdentityVerificationIntroScreen(
    onBackClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowStepScaffold(
        topBarTitle = stringResource(R.string.receiver_verify_title),
        actionButtonText = stringResource(R.string.receiver_verify_start_button),
        onBackClick = onBackClick,
        onActionClick = onStartClick,
        currentStep = ReceiverVerifyStep.IDENTITY,
        totalSteps = RECEIVER_VERIFY_TOTAL_STEPS,
        progressContentDescription = stringResource(R.string.receiver_verify_step_description, ReceiverVerifyStep.IDENTITY),
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(RECEIVER_VERIFY_HEADER_SPACING))
        Text(
            text = stringResource(R.string.receiver_verify_self_title),
            style = AfternoteDesign.typography.h1,
            color = AfternoteDesign.colors.black,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.receiver_verify_intro),
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray5,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IdentityVerificationIntroScreenPreview() {
    AfternoteTheme {
        IdentityVerificationIntroScreen(
            onBackClick = {},
            onStartClick = {},
        )
    }
}
