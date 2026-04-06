package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.model.DialogType
import com.afternote.feature.afternote.presentation.author.editor.processing.CustomServiceDialog
import com.afternote.feature.afternote.presentation.author.editor.processing.CustomServiceDialogCallbacks
import com.afternote.feature.afternote.presentation.author.editor.processing.CustomServiceDialogParams
import com.afternote.feature.afternote.presentation.author.editor.receiver.AddAfternoteEditorReceiverDialog
import com.afternote.feature.afternote.presentation.author.editor.receiver.AddAfternoteEditorReceiverDialogCallbacks
import com.afternote.feature.afternote.presentation.author.editor.receiver.AddAfternoteEditorReceiverDialogParams

@Composable
internal fun AfternoteEditorDialogs(state: AfternoteEditorState) {
    when (val dialogType = state.activeDialog) {
        null -> {
            Unit
        }

        DialogType.ADD_AFTERNOTE_EDIT_RECEIVER -> {
            AddAfternoteEditorReceiverDialog(
                params =
                    AddAfternoteEditorReceiverDialogParams(
                        afternoteEditReceiverNameState = state.afternoteEditReceiverNameState,
                        phoneNumberState = state.phoneNumberState,
                        relationshipSelectedValue = state.relationshipSelectedValue,
                        relationshipOptions = state.relationshipOptions,
                        callbacks =
                            AddAfternoteEditorReceiverDialogCallbacks(
                                onDismiss = state::dismissDialog,
                                onAddClick = state::onAddAfternoteEditorReceiver,
                                onRelationshipSelected = state::onRelationshipSelected,
                                onImportContactsClick = {
                                    // 연락처 가져오기 기능은 추후 구현 예정
                                },
                            ),
                    ),
            )
        }

        DialogType.CUSTOM_SERVICE -> {
            CustomServiceDialog(
                params =
                    CustomServiceDialogParams(
                        serviceNameState = state.customServiceNameState,
                        callbacks =
                            CustomServiceDialogCallbacks(
                                onDismiss = state::dismissDialog,
                                onAddClick = state::onAddCustomService,
                            ),
                    ),
            )
        }
    }
}
