package com.afternote.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier

fun Modifier.noRippleClickable(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier =
    this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
