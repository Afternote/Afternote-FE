package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun LabeledCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val color = AfternoteDesign.colors.black

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2f,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            if (checked) {
                drawCircle(
                    color = color,
                    radius = 9.dp.toPx(),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = AfternoteDesign.typography.bodySmallR,
        )
    }
}
