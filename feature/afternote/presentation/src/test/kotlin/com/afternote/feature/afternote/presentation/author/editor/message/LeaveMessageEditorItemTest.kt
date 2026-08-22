package com.afternote.feature.afternote.presentation.author.editor.message

import androidx.compose.foundation.text.input.TextFieldState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaveMessageEditorItemTest {
    @Test
    fun `본문 없는 말씀은 등록되지 않는다`() {
        val state = editorState()
        val message = state.editorMessages.single()
        message.titleState.edit { replace(0, length, "제목만") }

        state.registerEditorMessage(message)

        assertEquals(LeaveMessageEditorItemState.EDITING, message.state)
        assertEquals(1, state.editorMessages.size)
    }

    @Test
    fun `말씀을 등록해도 빈 입력 항목을 자동으로 추가하지 않는다`() {
        val state = editorState()
        val message = state.editorMessages.single()
        message.contentState.edit { replace(0, length, "전하고 싶은 말") }

        state.registerEditorMessage(message)

        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, message.state)
        assertEquals(1, state.editorMessages.size)
    }

    @Test
    fun `추가 버튼을 눌렀을 때만 새 빈 입력 항목을 추가한다`() {
        val state = editorState()

        state.addEditorMessage()

        assertEquals(2, state.editorMessages.size)
        assertEquals(LeaveMessageEditorItemState.EDITING, state.editorMessages.last().state)
        assertTrue(
            state.editorMessages
                .last()
                .contentState.text
                .isEmpty(),
        )
    }

    @Test
    fun `빈 입력 항목을 삭제해 등록된 말씀만 남아도 새 항목을 만들지 않는다`() {
        val state = editorState()
        val registered = state.editorMessages.single()
        registered.contentState.edit { replace(0, length, "전하고 싶은 말") }
        state.registerEditorMessage(registered)
        state.addEditorMessage()
        val editable = state.editorMessages.last()

        state.removeEditorMessage(editable)

        assertEquals(1, state.editorMessages.size)
        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, state.editorMessages.single().state)
    }

    @Test
    fun `등록된 말씀을 복원해도 빈 입력 항목을 자동으로 추가하지 않는다`() {
        val state = editorState()

        state.syncEditorMessagesFromForm(
            listOf(
                EditorMessageTextBlock(
                    title = "가족에게",
                    body = "전하고 싶은 말",
                    isRegistered = true,
                ),
            ),
        )

        assertEquals(1, state.editorMessages.size)
        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, state.editorMessages.single().state)
    }

    @Test
    fun `등록된 말씀만 펼치고 접을 수 있다`() {
        val message = LeaveMessageEditorItem(contentState = TextFieldState("전하고 싶은 말"))

        message.toggleBodyVisibility()
        assertEquals(LeaveMessageEditorItemState.EDITING, message.state)

        assertTrue(message.tryRegister())
        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, message.state)
        message.toggleBodyVisibility()
        assertEquals(LeaveMessageEditorItemState.REGISTERED_EXPANDED, message.state)
        message.toggleBodyVisibility()
        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, message.state)
    }

    private fun editorState() =
        AfternoteEditorState(
            idState = TextFieldState(),
            passwordState = TextFieldState(),
            customServiceNameState = TextFieldState(),
            getCurrentForm = { EditorFormState() },
            setType = {},
            setService = {},
            setMemorialPhoto = {},
            setMemorialVideo = {},
            addReceiverIfAbsent = { _, _, _ -> },
            applyPrefill = {},
            setMemorialThumbnail = {},
            deleteReceiver = {},
            replaceReceiversIfEmpty = {},
            setLeaveMessageBlocks = {},
            addProcessingMethod = {},
            deleteProcessingMethod = {},
            editProcessingMethod = { _, _ -> },
        )
}
