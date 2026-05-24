package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun MemorySpaceGridBackground(
    modifier: Modifier = Modifier,
    cellSize: Dp = 48.dp,
    strokeWidth: Dp = 1.dp,
    gridColor: Color = AfternoteDesign.colors.gray3,
) {
    Canvas(
        modifier =
            modifier.drawWithCache {
                val cellPx = cellSize.toPx()
                val strokePx = strokeWidth.toPx()

                onDrawBehind {
                    var xPos = 0f
                    while (xPos <= size.width) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x = xPos, y = 0f),
                            end = Offset(x = xPos, y = size.height),
                            strokeWidth = strokePx,
                        )
                        xPos += cellPx
                    }

                    var yPos = 0f
                    while (yPos <= size.height) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x = 0f, y = yPos),
                            end = Offset(x = size.width, y = yPos),
                            strokeWidth = strokePx,
                        )
                        yPos += cellPx
                    }
                }
            },
    ) {}
}

@Preview
@Composable
private fun MemorySpaceGridBackgroundPreview() {
    MemorySpaceGridBackground()
}
