package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.author.editor.processing.CustomServiceDialog
import com.afternote.feature.afternote.presentation.author.editor.receiver.AddAfternoteEditorReceiverDialog
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.DialogType

@Composable
internal fun AfternoteEditorDialogs(state: AfternoteEditorState) {
    when (state.activeDialog) {
        null -> {}

        DialogType.ADD_AFTERNOTE_EDIT_RECEIVER -> {
            AddAfternoteEditorReceiverDialog(
                afternoteEditReceiverNameState = state.afternoteEditReceiverNameState,
                phoneNumberState = state.phoneNumberState,
                relationshipSelectedValue = state.relationshipSelectedValue,
                relationshipOptions = editorRelationshipOptions(),
                onDismiss = state::dismissDialog,
                onAddClick = state::onAddAfternoteEditorReceiver,
                onRelationshipSelected = state::onRelationshipSelected,
                onImportContactsClick = {
                    // 연락처 가져오기 기능은 추후 구현 예정
                },
            )
        }

        DialogType.CUSTOM_SERVICE -> {
            CustomServiceDialog(
                serviceNameState = state.customServiceNameState,
                onDismiss = state::dismissDialog,
                onAddClick = state::onAddCustomService,
            )
        }
    }
}
