package com.afternote.core.ui.scaffold

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 단계형 흐름(회원가입·수신자 인증·기타 마법사) 의 상단 진행 인디케이터 (#221).
 *
 * 기존 feature 별로 분산되어 있던 `StepProgressBar` (onboarding) ·
 * `ReceiverVerifyStepProgressBar` (receiver/deliveryverification) 를 `core/ui` 로 통합.
 *
 * 디자인 토큰: 높이 4dp · gray3 트랙 · gray9 채움 · CircleShape · 500ms FastOutSlowIn animate.
 *
 * @param currentStep 현재 단계 (1..totalSteps). 0 이하면 0%, totalSteps 이상이면 100% 로 coerce.
 * @param totalSteps 전체 단계 수. 1 이상.
 * @param contentDescription 접근성 라벨. null 이면 semantics 미부여.
 */
@Composable
fun FlowStepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    // 진행률 계산 0..1. `.toFloat()` 으로 정수 나눗셈 회피, `coerceAtLeast(1)` 로 0 나눗셈 보호.
    val targetFraction = currentStep.toFloat() / totalSteps.coerceAtLeast(1)
    // currentStep 변화 시 단계 점프 없이 500ms 동안 가속·감속(Material 표준 FastOutSlowIn) 으로 부드럽게 채움.
    // `coerceIn(0f, 1f)` 는 호출자 실수(음수·범위 초과) 에도 LinearProgressIndicator 깨지지 않게 가드.
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "FlowStepProgressBarAnimation",
    )

    val semanticsModifier =
        if (contentDescription != null) {
            Modifier.semantics { this.contentDescription = contentDescription }
        } else {
            Modifier
        }

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier =
            modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    color = AfternoteDesign.colors.gray3,
                    shape = CircleShape,
                ).then(semanticsModifier),
        color = AfternoteDesign.colors.gray9,
        trackColor = Color.Transparent,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}
