package com.afternote.feature.setting.presentation.component

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PasswordDots(
    passwordLength: Int,
    totalDots: Int = 4,
    dotSize: Dp = 15.dp,
    dotSpacing: Dp = 18.dp,
    strokeWidth: Dp = 2.dp,
    filledColor: Color = Color(0xFF555555),
    emptyColor: Color = Color(0xFF555555),
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalDots) { index ->
            val isFilled = index < passwordLength
            Canvas(modifier = Modifier.size(dotSize)) {
                if (isFilled) {
                    drawCircle(
                        color = filledColor,
                        radius = size.minDimension / 2,
                    )
                } else {
                    drawCircle(
                        color = emptyColor,
                        radius = size.minDimension / 2 - strokeWidth.toPx() / 2,
                        style = Stroke(width = strokeWidth.toPx()),
                    )
                }
            }
        }
    }
}
