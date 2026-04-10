// [WIP / 소속 미확정] MemorySpace — UI 컴포넌트(격자 배경).
// 경로: feature/mindrecord/presentation/.../component/memoryspace/

package com.afternote.feature.mindrecord.presentation.component.memoryspace

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun MemorySpaceGridBackground(modifier: Modifier = Modifier) {
    val lineColor = AfternoteDesign.colors.gray3.copy(alpha = 0.45f)
    Canvas(modifier = modifier) {
        val gridCount = 10
        val cellWidth = size.width / gridCount
        val cellHeight = size.height / gridCount
        for (i in 0..gridCount) {
            drawLine(
                color = lineColor,
                start = Offset(i * cellWidth, 0f),
                end = Offset(i * cellWidth, size.height),
                strokeWidth = 1f,
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, i * cellHeight),
                end = Offset(size.width, i * cellHeight),
                strokeWidth = 1f,
            )
        }
    }
}
