package com.afternote.feature.afternote.presentation.receiver.list

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AppNoteItem(
    val name: String,
    val date: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color,
)
