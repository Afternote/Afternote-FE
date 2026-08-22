package com.afternote.feature.afternote.presentation.author.editor.message

import androidx.compose.foundation.text.input.TextFieldState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorUiHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaveMessageEditorItemTest {
    @Test
    fun `본문 없는 말씀은 등록되지 않는다`() {
        val holder = editorUiHolder()
        val message = holder.editorMessages.single()
        message.titleState.edit { replace(0, length, "제목만") }

        assertFalse(holder.registerEditorMessage(message))
        assertEquals(LeaveMessageEditorItemState.EDITING, message.state)
        assertEquals(1, holder.editorMessages.size)
    }

    @Test
    fun `마지막 편집 블록을 등록하면 새 빈 입력 블록을 남긴다`() {
        val holder = editorUiHolder()
        val message = holder.editorMessages.single()
        message.contentState.edit { replace(0, length, "전하고 싶은 말") }

        assertTrue(holder.registerEditorMessage(message))
        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, message.state)
        assertEquals(2, holder.editorMessages.size)
        assertEquals(LeaveMessageEditorItemState.EDITING, holder.editorMessages.last().state)
        assertTrue(
            holder.editorMessages
                .last()
                .contentState.text
                .isEmpty(),
        )
    }

    @Test
    fun `다른 편집 블록이 있으면 등록 시 빈 블록을 중복 추가하지 않는다`() {
        val holder = editorUiHolder()
        holder.addEditorMessage()
        val message = holder.editorMessages.first()
        message.contentState.edit { replace(0, length, "전하고 싶은 말") }

        assertTrue(holder.registerEditorMessage(message))
        assertEquals(2, holder.editorMessages.size)
    }

    @Test
    fun `등록된 말씀만 남도록 삭제해도 빈 입력 블록을 다시 보장한다`() {
        val holder = editorUiHolder()
        val registered = holder.editorMessages.single()
        registered.contentState.edit { replace(0, length, "전하고 싶은 말") }
        holder.registerEditorMessage(registered)
        val editable = holder.editorMessages.last()

        holder.removeEditorMessage(editable)

        assertEquals(2, holder.editorMessages.size)
        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, holder.editorMessages.first().state)
        assertEquals(LeaveMessageEditorItemState.EDITING, holder.editorMessages.last().state)
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

    private fun editorUiHolder() =
        AfternoteEditorUiHolder(
            idState = TextFieldState(),
            passwordState = TextFieldState(),
            customServiceNameState = TextFieldState(),
        )
}
