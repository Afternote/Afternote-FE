package com.afternote.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * 가로 스크롤 영역 페이드 마스크를 적용할 방향.
 */
enum class FadingEdgeDirection {
    LEFT,
    RIGHT,
    BOTH,
}

/**
 * LazyRow나 가로 스크롤 가능한 컨테이너 끝에 페이드 아웃 효과를 추가하는 Modifier
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
