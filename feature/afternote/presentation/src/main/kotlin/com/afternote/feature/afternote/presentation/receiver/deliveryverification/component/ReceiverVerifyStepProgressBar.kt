package com.afternote.feature.afternote.presentation.receiver.deliveryverification.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 수신자 인증 흐름(designs 2·3·4·5·6·7·8) 의 상단 진행 인디케이터.
 *
 * 발신자 온보딩의 [com.afternote.feature.onboarding.presentation.signup.scaffold.StepProgressBar] 와
 * 동일 디자인이지만 onboarding 모듈 cross-feature import 가 불가해 별도 정의한다. 단계 수는
 * 본인 확인(1) → 마스터 키(2) → 서류(3) 의 3 단계.
 *
 * 추후 core/ui 로 추출해 공용화하면 본 파일은 제거 대상.
 */
@Composable
fun ReceiverVerifyStepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    val targetFraction = currentStep.toFloat() / totalSteps.coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "ReceiverVerifyStepProgressBarAnimation",
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier =
            modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    color = AfternoteDesign.colors.gray3,
                    shape = CircleShape,
                ),
        color = AfternoteDesign.colors.gray9,
        trackColor = Color.Transparent,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun ReceiverVerifyStepProgressBarPreview() {
    AfternoteTheme {
        ReceiverVerifyStepProgressBar(currentStep = 1, totalSteps = 3)
    }
}
