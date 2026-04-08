package com.afternote.core.ui

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Figma 스펙에 맞춘 드롭 섀도(색·spread·blur·offset).
 *
 * - Spread는 상하좌우 균등(크기 + 2×spread, translate로 중심 정렬).
 * - [drawWithCache]로 크기·파라미터 구간에서 Paint/Outline 재사용.
 */
fun Modifier.dropShadow(
    shape: Shape,
    color: Color = Color.Black.copy(alpha = 0.25f),
    blur: Dp = 1.dp,
    offsetY: Dp = 1.dp,
    offsetX: Dp = 1.dp,
    spread: Dp = 1.dp,
) = this.drawWithCache {
    val blurPx = blur.toPx()
    val spreadPx = spread.toPx()
    val offsetXPx = offsetX.toPx()
    val offsetYPx = offsetY.toPx()

    val paint =
        Paint().apply {
            this.color = color
            if (blurPx > 0f) {
                asFrameworkPaint().maskFilter =
                    BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
        }

    val shadowWidth = size.width + (spreadPx * 2)
    val shadowHeight = size.height + (spreadPx * 2)

    val shadowOutline =
        if (shadowWidth > 0f && shadowHeight > 0f) {
            shape.createOutline(Size(shadowWidth, shadowHeight), layoutDirection, this)
        } else {
            null
        }

    onDrawBehind {
        if (shadowOutline != null) {
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(
                    dx = offsetXPx - spreadPx,
                    dy = offsetYPx - spreadPx,
                )
                canvas.drawOutline(shadowOutline, paint)
                canvas.restore()
            }
        }
    }
}
