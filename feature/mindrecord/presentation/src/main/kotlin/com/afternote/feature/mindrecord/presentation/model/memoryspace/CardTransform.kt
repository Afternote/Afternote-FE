package com.afternote.feature.mindrecord.presentation.model.memoryspace

import androidx.compose.ui.unit.Dp

data class CardTransform(
    val offsetX: Dp,
    val offsetY: Dp,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val zIndex: Float = 0f,
)
