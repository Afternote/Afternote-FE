package com.afternote.feature.afternote.presentation.author.editor.receiver
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable

/**
 * AddAfternoteEditorReceiverDialog 함수의 매개변수를 묶는 data class
 */
@Immutable
data class AddAfternoteEditorReceiverDialogParams(
    val afternoteEditReceiverNameState: TextFieldState,
    val phoneNumberState: TextFieldState,
    val relationshipSelectedValue: String,
    val relationshipOptions: List<String>,
    val callbacks: AddAfternoteEditorReceiverDialogCallbacks = AddAfternoteEditorReceiverDialogCallbacks(),
)

/**
 * AddAfternoteEditorReceiverDialog의 콜백들을 묶는 data class
 */
@Immutable
data class AddAfternoteEditorReceiverDialogCallbacks(
    val onDismiss: () -> Unit = {},
    val onAddClick: () -> Unit = {},
    val onRelationshipSelected: (String) -> Unit = {},
    val onImportContactsClick: () -> Unit = {},
)
