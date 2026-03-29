package com.afternote.feature.mindrecord.presentation.model

import androidx.compose.ui.text.style.TextAlign

data class TextStyleState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val textAlign: TextAlign = TextAlign.Start,
    val textStyle: TextStyleType = TextStyleType.BODY,
)

enum class TextStyleType { TITLE, HEADER, SUBHEADER, BODY }