package com.afternote.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.toggleShadow_1
import com.afternote.core.ui.theme.toggleShadow_2
import kotlin.math.roundToInt

@Composable
fun ViewModeSwitcher(
    isListView: Boolean,
    onViewChange: (Boolean) -> Unit,
    image1: Int,
    image2: Int,
    modifier: Modifier = Modifier,
) {
    val containerHeight = 36.dp
    val containerWidth = 68.dp
    val indicatorPadding = 4.dp
    val indicatorSize = containerHeight - (indicatorPadding * 2)

    val targetOffset =
        if (isListView) {
            0f
        } else {
            with(LocalDensity.current) {
                (containerWidth - indicatorSize - (indicatorPadding * 2)).toPx()
            }
        }

    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "IndicatorOffset",
    )

    Box(
        modifier =
            modifier
                .size(width = containerWidth, height = containerHeight)
                .clip(CircleShape)
                .background(Gray2)
                .padding(indicatorPadding),
    ) {
        Box(
            modifier =
                Modifier
                    .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                    .size(indicatorSize)
                    .dropShadow(
                        shape = CircleShape,
                        shadow = toggleShadow_1,
                    ).dropShadow(
                        shape = CircleShape,
                        shadow = toggleShadow_2,
                    ).background(Color.White, CircleShape),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            SwitcherIcon(
                icon = painterResource(image1),
                onClick = { onViewChange(true) },
            )
            SwitcherIcon(
                icon = painterResource(image2),
                onClick = { onViewChange(false) },
            )
        }
    }
}

@Composable
private fun RowScope.SwitcherIcon(
    icon: Painter,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Black.copy(alpha = 0.6f),
        )
    }
}
