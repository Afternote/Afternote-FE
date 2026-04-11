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

@Composable
fun StepProgressBar(
    currentStep: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = currentStep.toFloat() / 4,
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
        StepProgressBar(currentStep = 1)
    }
}

@Preview(showBackground = true, name = "StepProgressBar - 3/4")
@Composable
private fun StepProgressBarStep3Preview() {
    AfternoteTheme {
        StepProgressBar(currentStep = 3)
    }
}
