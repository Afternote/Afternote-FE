package com.afternote.core.ui.modifierextention

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

/**
 * Shimmer 그라데이션의 두 색 — 본 Modifier 가 `Modifier.Node` 라 Composable 컨텍스트 밖이라
 * `AfternoteDesign.colors` 직접 접근 불가. 디자인 시스템 토큰화 + 다크 모드 대응은
 * `shimmerLoadingPlaceholder(highlight, base)` 시그니처 변경 + 호출처 전수 갱신을 동반하는
 * 별 PR. 본 const 는 매직 넘버 → 이름 부여까지의 중간 단계.
 */
private val ShimmerBase = Color(0xFFE8E8E8)
private val ShimmerHighlight = Color(0xFFF6F6F6)

/**
 * 이미지·썸네일 로딩 구간에 쓰는 가벼운 shimmer 배경.
 * [Modifier.composed] 대신 [Modifier.Node]로 그리기만 갱신해 리컴포지션·할당을 줄인다.
 */
fun Modifier.shimmerLoadingPlaceholder(): Modifier = this then ShimmerLoadingPlaceholderElement

private object ShimmerLoadingPlaceholderElement : ModifierNodeElement<ShimmerLoadingPlaceholderNode>() {
    override fun create(): ShimmerLoadingPlaceholderNode = ShimmerLoadingPlaceholderNode()

    override fun update(node: ShimmerLoadingPlaceholderNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmerLoadingPlaceholder"
    }

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = System.identityHashCode(this)
}

private class ShimmerLoadingPlaceholderNode :
    Modifier.Node(),
    DrawModifierNode,
    LayoutAwareModifierNode {
    private var widthPx = 1f
    private val progress = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            while (isAttached) {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1100, easing = LinearEasing),
                )
                progress.snapTo(0f)
            }
        }
    }

    override fun onRemeasured(size: IntSize) {
        widthPx = size.width.toFloat().coerceAtLeast(1f)
    }

    override fun ContentDrawScope.draw() {
        val current = progress.value
        val startX = (current * 2f - 0.5f) * widthPx
        val brush =
            Brush.linearGradient(
                colorStops =
                    arrayOf(
                        0f to ShimmerBase,
                        0.4f to ShimmerHighlight,
                        0.6f to ShimmerHighlight,
                        1f to ShimmerBase,
                    ),
                start = Offset(startX, 0f),
                end = Offset(startX + widthPx, widthPx),
            )
        drawRect(brush = brush)
        drawContent()
    }
}
