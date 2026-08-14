package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.author.editor.processing.CustomServiceDialog
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.DialogType

@Composable
internal fun AfternoteEditorDialogs(state: AfternoteEditorState) {
    when (state.activeDialog) {
        null -> {}

        DialogType.CUSTOM_SERVICE -> {
            CustomServiceDialog(
                serviceNameState = state.customServiceNameState,
                onDismiss = state::dismissDialog,
                onAddClick = state::onAddCustomService,
            )
        }
    }
}
