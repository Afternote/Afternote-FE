package com.afternote.feature.afternote.presentation.author.editor.message

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import java.util.UUID

/**
 * 남기실 말씀 항목 하나를 나타내는 모델
 */
@Stable
class EditorMessage(
    val id: String = UUID.randomUUID().toString(),
    val titleState: TextFieldState = TextFieldState(),
    val contentState: TextFieldState = TextFieldState(),
)
