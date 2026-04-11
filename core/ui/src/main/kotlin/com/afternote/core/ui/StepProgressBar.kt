package com.afternote.core.ui

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 회원가입 등 다단계 플로우에서 사용하는 진행바.
 *
 *   M3 기본 트랙/스탑 인디케이터는 숨겨 디자인 가이드와 일치시킵니다.
 * - [currentStep] 변경 시 500ms `FastOutSlowInEasing` 으로 부드럽게 차오릅니다.
 *
 * @param currentStep 현재 단계 (1부터 시작)
 * @param totalSteps 전체 단계 수
 * @param modifier 외부에서 적용할 Modifier
 * @param contentDescription 스크린리더에 읽힐 설명. null 이면 semantics 를 추가하지 않습니다.
 */
@Composable
fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = currentStep.toFloat() / totalSteps,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "StepProgressBarAnimation",
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

@Preview(showBackground = true, name = "StepProgressBar - 1/4")
@Composable
private fun StepProgressBarStep1Preview() {
    AfternoteTheme {
        StepProgressBar(currentStep = 1, totalSteps = 4)
    }
}

@Preview(showBackground = true, name = "StepProgressBar - 3/4")
@Composable
private fun StepProgressBarStep3Preview() {
    AfternoteTheme {
        StepProgressBar(currentStep = 3, totalSteps = 4)
    }
}
