package com.afternote.core.ui.theme

import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

val toggleShadow_1: Shadow =
    Shadow(
        radius = 3.dp,
        spread = 0.dp,
        color = Black.copy(alpha = 0.1f),
        offset = DpOffset(x = 0.dp, 1.dp),
    )

val toggleShadow_2: Shadow =
    Shadow(
        radius = 2.dp,
        spread = (-1).dp,
        color = Black.copy(alpha = 0.1f),
        offset = DpOffset(x = 0.dp, 1.dp),
    )
