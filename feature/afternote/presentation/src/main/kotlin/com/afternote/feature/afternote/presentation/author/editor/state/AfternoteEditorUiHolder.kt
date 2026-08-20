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
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage

/**
 * 다이얼로그 타입 (순수 UI).
 */
enum class DialogType {
    CUSTOM_SERVICE,
}

/**
 * 에디터 **순수 UI** 상태: 탭·다이얼로그·드롭다운·텍스트 필드·멀티 메시지 입력 UI.
 * 프로세스가 죽어도 복원되지 않아도 되는 휘발성 상태를 둔다.
 *
 * 추억 플레이리스트 곡 목록은 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] SSOT가 보유하며,
 * 본 UI 상태는 곡 목록을 직접 들고 있지 않는다.
 */
@Stable
class AfternoteEditorUiHolder(
    val idState: TextFieldState,
    val passwordState: TextFieldState,
    val customServiceNameState: TextFieldState,
) {
    val editorMessages: SnapshotStateList<EditorMessage> =
        mutableStateListOf(EditorMessage())

    var activeDialog by mutableStateOf<DialogType?>(null)
        private set

    var typeDropdownExpanded by mutableStateOf(false)
        private set

    var serviceDropdownExpanded by mutableStateOf(false)
        private set

    fun addEditorMessage() {
        editorMessages.add(EditorMessage())
    }

    fun removeEditorMessage(message: EditorMessage) {
        if (editorMessages.size > 1) {
            editorMessages.removeAll { it.id == message.id }
        }
    }

    fun showCustomServiceDialog() {
        activeDialog = DialogType.CUSTOM_SERVICE
    }

    fun dismissDialog() {
        activeDialog = null
        customServiceNameState.edit { replace(0, length, "") }
    }

    fun onTypeDropdownExpandedChange(expanded: Boolean) {
        typeDropdownExpanded = expanded
    }

    fun onServiceDropdownExpandedChange(expanded: Boolean) {
        serviceDropdownExpanded = expanded
    }
}

@Composable
fun rememberAfternoteEditorUiHolder(
    idState: TextFieldState,
    passwordState: TextFieldState,
    customServiceNameState: TextFieldState,
): AfternoteEditorUiHolder =
    remember(
        idState,
        passwordState,
        customServiceNameState,
    ) {
        AfternoteEditorUiHolder(
            idState = idState,
            passwordState = passwordState,
            customServiceNameState = customServiceNameState,
        )
    }
