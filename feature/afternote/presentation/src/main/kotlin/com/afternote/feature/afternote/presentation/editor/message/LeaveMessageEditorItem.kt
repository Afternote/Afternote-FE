package com.afternote.feature.afternote.presentation.editor.message

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

enum class LeaveMessageEditorItemState {
    EDITING,
    REGISTERED_COLLAPSED,
    REGISTERED_EXPANDED,
}

/**
 * 남기실 말씀 항목 하나를 나타내는 모델
 */
@Stable
class LeaveMessageEditorItem(
    val id: String = UUID.randomUUID().toString(),
    val titleState: TextFieldState = TextFieldState(),
    val contentState: TextFieldState = TextFieldState(),
    initialState: LeaveMessageEditorItemState = LeaveMessageEditorItemState.EDITING,
) {
    /** 편집 중인지, 등록 후 본문이 접혔는지 또는 펼쳐졌는지를 함께 나타낸다. */
    var state by mutableStateOf(initialState)
        private set

    val isRegistered: Boolean
        get() = state != LeaveMessageEditorItemState.EDITING

    val isBodyVisible: Boolean
        get() = state == LeaveMessageEditorItemState.REGISTERED_EXPANDED

    /** 본문 없는 블록은 등록하지 않고 편집 상태에 남긴다. 서버 저장은 전체 폼 저장 시 수행한다. */
    fun tryRegister(): Boolean {
        if (state != LeaveMessageEditorItemState.EDITING || contentState.text.isBlank()) return false
        state = LeaveMessageEditorItemState.REGISTERED_COLLAPSED
        return true
    }

    fun toggleBodyVisibility() {
        state =
            when (state) {
                LeaveMessageEditorItemState.EDITING -> {
                    LeaveMessageEditorItemState.EDITING
                }

                LeaveMessageEditorItemState.REGISTERED_COLLAPSED -> {
                    LeaveMessageEditorItemState.REGISTERED_EXPANDED
                }

                LeaveMessageEditorItemState.REGISTERED_EXPANDED -> {
                    LeaveMessageEditorItemState.REGISTERED_COLLAPSED
                }
            }
    }
}
