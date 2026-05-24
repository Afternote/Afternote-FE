package com.afternote.core.ui.modifierextention

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// =====================================================================
// bottomBorder
// =====================================================================

/**
 * 하단 보더를 그리는 Modifier 확장 함수.
 *
 * @param color 보더 색상
 * @param width 보더 두께 (기본값: 1.dp)
 */
fun Modifier.bottomBorder(
    color: Color,
    width: Dp = 1.dp,
) = this.then(
    if (width > 0.dp && color != Color.Transparent) {
        Modifier.drawBehind {
            val borderWidth = width.toPx()
            val y = size.height - (borderWidth / 2f)

            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = borderWidth,
            )
        }
    } else {
        Modifier
    },
)

// =====================================================================
// dropShadow
// =====================================================================

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

// =====================================================================
// horizontalFadingEdge
// =====================================================================

/**
 * 가로 스크롤 영역 페이드 마스크를 적용할 방향.
 */
enum class FadingEdgeDirection {
    LEFT,
    RIGHT,
    BOTH,
}

/**
 * LazyRow나 가로 스크롤 가능한 컨테이너 끝에 페이드 아웃 효과를 추가하는 Modifier.
 *
 * Alpha Masking([BlendMode.DstIn])으로 배경이 아닌 **콘텐츠 알파**를 줄입니다.
 * [CompositingStrategy.Offscreen]으로 오프스크린 합성해 다크 모드·그라데이션 배경에서도 자연스럽게 동작합니다.
 *
 * **Modifier 적용 순서 중요:**
 * - `horizontalFadingEdge` → `horizontalScroll` → `padding` 순으로 두면 페이드가 화면에 고정되고 스크롤 콘텐츠만 움직입니다.
 *
 * @param edgeWidth 페이드 아웃 영역 너비 (보통 24dp ~ 48dp)
 * @param direction 페이드를 줄 가장자리 (기본: 오른쪽만 — 기존 호출부와 동일)
 */
fun Modifier.horizontalFadingEdge(
    edgeWidth: Dp,
    direction: FadingEdgeDirection = FadingEdgeDirection.RIGHT,
) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val edgeWidthPx = edgeWidth.toPx()

        if (direction == FadingEdgeDirection.RIGHT || direction == FadingEdgeDirection.BOTH) {
            drawRect(
                brush =
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = size.width - edgeWidthPx,
                        endX = size.width,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }

        if (direction == FadingEdgeDirection.LEFT || direction == FadingEdgeDirection.BOTH) {
            drawRect(
                brush =
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startX = 0f,
                        endX = edgeWidthPx,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
