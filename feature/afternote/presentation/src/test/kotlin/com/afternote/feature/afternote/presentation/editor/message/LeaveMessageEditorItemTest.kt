package com.afternote.feature.afternote.presentation.editor.message

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaverScope
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.editorMessagesSaver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaveMessageEditorItemTest {
    @Test
    fun `초기 상태에는 빈 입력 항목을 자동으로 만들지 않는다`() {
        assertTrue(editorState().editorMessages.isEmpty())
    }

    @Test
    fun `본문 없는 말씀은 등록되지 않는다`() {
        val state = editorStateWithMessage()
        val message = state.editorMessages.single()
        message.titleState.edit { replace(0, length, "제목만") }

        state.registerEditorMessage(message)

        assertEquals(LeaveMessageEditorItemState.EDITING, message.state)
        assertEquals(1, state.editorMessages.size)
    }

    @Test
    fun `말씀을 등록해도 빈 입력 항목을 자동으로 추가하지 않는다`() {
        val state = editorStateWithMessage()
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

        assertEquals(1, state.editorMessages.size)
        assertEquals(LeaveMessageEditorItemState.EDITING, state.editorMessages.single().state)
        assertTrue(
            state.editorMessages
                .single()
                .contentState.text
                .isEmpty(),
        )
    }

    @Test
    fun `빈 입력 항목을 삭제해 등록된 말씀만 남아도 새 항목을 만들지 않는다`() {
        val state = editorStateWithMessage()
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
    fun `마지막 입력 항목을 삭제하면 빈 목록이 된다`() {
        val state = editorStateWithMessage()

        state.removeEditorMessage(state.editorMessages.single())

        assertTrue(state.editorMessages.isEmpty())
    }

    @Test
    fun `빈 프리필은 빈 목록을 유지한다`() {
        val state = editorStateWithMessage()

        state.replaceEditorMessages(emptyList())

        assertTrue(state.editorMessages.isEmpty())
    }

    @Test
    fun `등록된 말씀을 복원해도 빈 입력 항목을 자동으로 추가하지 않는다`() {
        val state = editorState()

        state.replaceEditorMessages(
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
    fun `목록 Saver는 입력과 등록 여부를 복원하고 펼침 상태는 접는다`() {
        val editing =
            LeaveMessageEditorItem(
                titleState = TextFieldState("작성 중"),
                contentState = TextFieldState("초안"),
            )
        val registered =
            LeaveMessageEditorItem(
                titleState = TextFieldState("가족에게"),
                contentState = TextFieldState("전하고 싶은 말"),
            )
        assertTrue(registered.tryRegister())
        registered.toggleBodyVisibility()
        val messages = mutableStateListOf(editing, registered)

        val saved = editorMessagesSaver.run { SaverScope { true }.save(messages) }
        val restored = requireNotNull(editorMessagesSaver.restore(requireNotNull(saved)))

        assertEquals(2, restored.size)
        assertEquals("작성 중", restored[0].titleState.text.toString())
        assertEquals("초안", restored[0].contentState.text.toString())
        assertEquals(LeaveMessageEditorItemState.EDITING, restored[0].state)
        assertEquals("가족에게", restored[1].titleState.text.toString())
        assertEquals("전하고 싶은 말", restored[1].contentState.text.toString())
        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, restored[1].state)
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
            serviceSearchQueryState = TextFieldState(),
            getCurrentForm = { EditorFormState() },
            setType = {},
            setService = {},
            setMemorialPhoto = {},
            removeMemorialPhoto = {},
            setMemorialVideo = {},
            removeMemorialVideo = {},
            addReceiverIfAbsent = { _, _, _ -> },
            applyPrefill = {},
            setMemorialThumbnail = {},
            deleteReceiver = {},
            replaceReceiversIfEmpty = {},
            addProcessingMethod = {},
            deleteProcessingMethod = {},
            editProcessingMethod = { _, _ -> },
        )

    private fun editorStateWithMessage() = editorState().apply { addEditorMessage() }
}
