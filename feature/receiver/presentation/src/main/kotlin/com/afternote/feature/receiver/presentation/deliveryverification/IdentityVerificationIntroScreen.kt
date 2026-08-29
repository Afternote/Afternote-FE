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
 * 수신자 본인 확인 안내(design 2) — 진행 인디케이터 1/3 + 안내 문구 + "인증 시작하기" CTA.
 *
 * 발신자 상세의 "열람 신청하기" 진입 시 해당 발신자의 본인 확인 캐시
 * ([com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository.isVerified]) 가
 * false 인 경우만 노출된다 — 캐시 hit 시 NavGraph 가 마스터 키 단계로 바로 보낸다 (#597 발신자별 격리).
 * 다음 단계의 인증번호 발송·검증은 실 API(`receiver-auth/email` 계열) 호출 (#407).
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
