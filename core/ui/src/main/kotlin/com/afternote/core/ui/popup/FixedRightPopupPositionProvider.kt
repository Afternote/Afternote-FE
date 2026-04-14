package com.afternote.core.ui.popup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

/**
 * 앵커(예: 더보기 버튼) **아래쪽**으로 [bottomGap] 만큼 띄우고,
 * 팝업의 **오른쪽**을 창의 오른쪽에서 [fixedRightMargin] 만큼 안쪽에 맞춘다.
 *
 * 위치 정책만 담당하며, 호출부에서 다른 [PopupPositionProvider] 로 교체할 수 있다.
 */
@Composable
fun rememberFixedRightPopupPositionProvider(
    bottomGap: Dp = 12.dp,
    fixedRightMargin: Dp = 15.dp,
): PopupPositionProvider {
    val density = LocalDensity.current
    return remember(bottomGap, fixedRightMargin, density) {
        val yGapPx = with(density) { bottomGap.roundToPx() }
        val rightMarginPx = with(density) { fixedRightMargin.roundToPx() }
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val x = windowSize.width - popupContentSize.width - rightMarginPx
                val y = anchorBounds.bottom + yGapPx
                return IntOffset(x, y)
            }
        }
    }
}
