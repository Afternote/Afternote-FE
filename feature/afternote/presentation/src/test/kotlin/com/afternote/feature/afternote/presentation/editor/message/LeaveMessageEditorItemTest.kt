package com.afternote.feature.afternote.presentation.editor.message

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.rememberAfternoteEditorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 남기실 말씀 목록의 소유자 계약.
 *
 * 프리필 반영은 [AfternoteEditorState.applyFormPrefill], 화면 재생성 보존은
 * `rememberAfternoteEditorState` 의 `rememberSaveable` 로 판정한다 — 둘 다 화면이 실제로 부르는
 * 진입점이고, 그 안의 목록 교체·Saver 는 구현 세부다 (#1673).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LeaveMessageEditorItemTest {
    @get:Rule
    val composeRule = createComposeRule()

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

        state.applyFormPrefill(prefill(messages = emptyList()))

        assertTrue(state.editorMessages.isEmpty())
    }

    @Test
    fun `등록된 말씀을 복원해도 빈 입력 항목을 자동으로 추가하지 않는다`() {
        val state = editorState()

        state.applyFormPrefill(
            prefill(
                messages =
                    listOf(
                        EditorMessageTextBlock(
                            title = "가족에게",
                            body = "전하고 싶은 말",
                            isRegistered = true,
                        ),
                    ),
            ),
        )

        assertEquals(1, state.editorMessages.size)
        assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, state.editorMessages.single().state)
    }

    @Test
    fun `화면 재생성에도 입력과 등록 여부는 남고 펼침 상태는 접힌다`() {
        val restorationTester = StateRestorationTester(composeRule)
        lateinit var state: AfternoteEditorState
        restorationTester.setContent { state = rememberAfternoteEditorState() }

        composeRule.runOnIdle {
            state.addEditorMessage()
            state.addEditorMessage()
            val editing = state.editorMessages[0]
            val registered = state.editorMessages[1]
            editing.titleState.edit { replace(0, length, "작성 중") }
            editing.contentState.edit { replace(0, length, "초안") }
            registered.titleState.edit { replace(0, length, "가족에게") }
            registered.contentState.edit { replace(0, length, "전하고 싶은 말") }
            state.registerEditorMessage(registered)
            registered.toggleBodyVisibility()
            assertEquals(LeaveMessageEditorItemState.REGISTERED_EXPANDED, registered.state)
        }

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle {
            val restored = state.editorMessages
            assertEquals(2, restored.size)
            assertEquals("작성 중", restored[0].titleState.text.toString())
            assertEquals("초안", restored[0].contentState.text.toString())
            assertEquals(LeaveMessageEditorItemState.EDITING, restored[0].state)
            assertEquals("가족에게", restored[1].titleState.text.toString())
            assertEquals("전하고 싶은 말", restored[1].contentState.text.toString())
            assertEquals(LeaveMessageEditorItemState.REGISTERED_COLLAPSED, restored[1].state)
        }
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

    private fun prefill(messages: List<EditorMessageTextBlock>) =
        EditorFormPrefill(
            content =
                EditorContentPrefill.Gallery(
                    serviceName = "구글 포토",
                    processingMethods = emptyList(),
                ),
            leaveMessageBlocks = messages,
            receivers = emptyList(),
        )

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
