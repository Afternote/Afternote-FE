package com.afternote.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

val shadow: Shadow =
    Shadow(
        radius = 5.dp,
        spread = 0.dp,
        color = Color(0xFF000000).copy(alpha = 0.05f),
        offset = DpOffset(x = 0.dp, 2.dp),
    )

val shadow_I: Shadow =
    Shadow(
        radius = 8.dp,
        spread = 0.dp,
        color = Color(0xFF000000).copy(alpha = 0.05f),
        offset = DpOffset(x = 0.dp, 1.dp),
    )

val toggleShadow_1: Shadow =
    Shadow(
        radius = 3.dp,
        spread = 0.dp,
        color = Color(0xFF000000).copy(alpha = 0.1f),
        offset = DpOffset(x = 0.dp, 1.dp),
    )

val toggleShadow_2: Shadow =
    Shadow(
        radius = 2.dp,
        spread = -1.dp,
        color = Color(0xFF000000).copy(alpha = 0.1f),
        offset = DpOffset(x = 0.dp, 1.dp),
    )

val emphasizeShadow: Shadow =
    Shadow(
        radius = 16.dp,
        spread = 0.dp,
        color = Color(0xFF000000).copy(alpha = 0.2f),
        offset = DpOffset(x = 0.dp, 4.dp),
    )
