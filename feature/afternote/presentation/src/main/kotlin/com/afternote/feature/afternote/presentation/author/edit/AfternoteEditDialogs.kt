package com.afternote.feature.afternote.presentation.author.edit

import androidx.compose.runtime.Composable
import com.afternote.feature.afternote.presentation.author.edit.model.AfternoteEditState
import com.afternote.feature.afternote.presentation.author.edit.model.DialogType
import com.afternote.feature.afternote.presentation.author.edit.processing.CustomServiceDialog
import com.afternote.feature.afternote.presentation.author.edit.processing.CustomServiceDialogCallbacks
import com.afternote.feature.afternote.presentation.author.edit.processing.CustomServiceDialogParams
import com.afternote.feature.afternote.presentation.author.edit.receiver.AddAfternoteEditReceiverDialog
import com.afternote.feature.afternote.presentation.author.edit.receiver.AddAfternoteEditReceiverDialogCallbacks
import com.afternote.feature.afternote.presentation.author.edit.receiver.AddAfternoteEditReceiverDialogParams

@Composable
internal fun AfternoteEditDialogs(state: AfternoteEditState) {
    when (val dialogType = state.activeDialog) {
        null -> Unit
        DialogType.ADD_AFTERNOTE_EDIT_RECEIVER -> {
            AddAfternoteEditReceiverDialog(
                params =
                    AddAfternoteEditReceiverDialogParams(
                        afternoteEditReceiverNameState = state.afternoteEditReceiverNameState,
                        phoneNumberState = state.phoneNumberState,
                        relationshipSelectedValue = state.relationshipSelectedValue,
                        relationshipOptions = state.relationshipOptions,
                        callbacks =
                            AddAfternoteEditReceiverDialogCallbacks(
                                onDismiss = state::dismissDialog,
                                onAddClick = state::onAddAfternoteEditReceiver,
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
