package com.afternote.feature.afternote.presentation.author.editor.state

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.afternote.core.ui.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdownState

/**
 * 다이얼로그 타입 (순수 UI).
 */
enum class DialogType {
    ADD_AFTERNOTE_EDIT_RECEIVER,
    CUSTOM_SERVICE,
}

/**
 * 에디터 **순수 UI** 상태: 탭·다이얼로그·드롭다운·텍스트 필드·멀티 메시지 입력 UI.
 * 프로세스가 죽어도 복원되지 않아도 되는 휘발성 상태를 둔다.
 */
@Stable
class AfternoteEditorUiState(
    val idState: TextFieldState,
    val passwordState: TextFieldState,
    val afternoteEditReceiverNameState: TextFieldState,
    val phoneNumberState: TextFieldState,
    val customServiceNameState: TextFieldState,
    val customLastWishState: TextFieldState,
) {
    val editorMessages: SnapshotStateList<EditorMessage> =
        mutableStateListOf(EditorMessage())

    var selectedBottomNavItem by mutableStateOf(BottomNavTab.NOTE)
        private set

    var relationshipSelectedValue by mutableStateOf("친구")
        private set

    var activeDialog by mutableStateOf<DialogType?>(null)
        private set

    var playlistStateHolder: MemorialPlaylistStateHolder? = null
        private set

    var categoryDropdownState by mutableStateOf(SelectionDropdownState())
        private set

    var serviceDropdownState by mutableStateOf(SelectionDropdownState())
        private set

    @Suppress("UNUSED")
    var relationshipDropdownState by mutableStateOf(SelectionDropdownState())
        private set

    fun addEditorMessage() {
        editorMessages.add(EditorMessage())
    }

    fun removeEditorMessage(message: EditorMessage) {
        if (editorMessages.size > 1) {
            editorMessages.removeAll { it.id == message.id }
        }
    }

    fun setPlaylistStateHolder(stateHolder: MemorialPlaylistStateHolder) {
        playlistStateHolder = stateHolder
    }

    fun showAddAfternoteEditorReceiverDialog() {
        activeDialog = DialogType.ADD_AFTERNOTE_EDIT_RECEIVER
    }

    fun showCustomServiceDialog() {
        activeDialog = DialogType.CUSTOM_SERVICE
    }

    fun dismissDialogInternal(clearReceiverFields: () -> Unit) {
        activeDialog = null
        clearReceiverFields()
        relationshipSelectedValue = "친구"
    }

    fun onBottomNavItemSelected(item: BottomNavTab) {
        selectedBottomNavItem = item
    }

    fun onRelationshipSelected(relationship: String) {
        relationshipSelectedValue = relationship
    }
}

@Composable
fun rememberAfternoteEditorUiState(
    idState: TextFieldState,
    passwordState: TextFieldState,
    afternoteEditReceiverNameState: TextFieldState,
    phoneNumberState: TextFieldState,
    customServiceNameState: TextFieldState,
    customLastWishState: TextFieldState,
): AfternoteEditorUiState =
    remember(
        idState,
        passwordState,
        afternoteEditReceiverNameState,
        phoneNumberState,
        customServiceNameState,
        customLastWishState,
    ) {
        AfternoteEditorUiState(
            idState = idState,
            passwordState = passwordState,
            afternoteEditReceiverNameState = afternoteEditReceiverNameState,
            phoneNumberState = phoneNumberState,
            customServiceNameState = customServiceNameState,
            customLastWishState = customLastWishState,
        )
    }
