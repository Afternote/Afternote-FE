package com.afternote.feature.afternote.presentation.author.editor

import androidx.compose.foundation.text.input.TextFieldState
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorUiHolder
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 이탈 가드 상태 지문([editorContentSignature])의 완전성 계약 테스트.
 *
 * [AfternoteEditorUiHolder] 의 [TextFieldState] 를 리플렉션으로 전수 열거한 뒤 하나씩 실제로
 * 입력해 보고 지문이 반응하는지 검사한다. 홀더에 입력 상태가 새로 생기면 이 테스트가 자동으로
 * 그 필드를 집어 들므로, 지문에 반영하거나 [dialogTransientStates] 에 사유와 함께 올리는 결정이
 * 강제된다. 폼 쪽은 통째 직렬화 구조라 필드 추가가 자동 포함 — 카나리만 둔다.
 */
class EditorContentSignatureCompletenessTest {
    /**
     * 다이얼로그 전용 휘발 입력 — dismissDialog 가 비우고 다이얼로그가 자체 back 을 소비하므로
     * 지문에서 제외한다. 여기 올리려면 같은 성질(닫힐 때 소거되는 입력)이어야 한다.
     */
    private val dialogTransientStates =
        setOf("afternoteEditReceiverNameState", "phoneNumberState", "customServiceNameState")

    private fun newState(): AfternoteEditorState =
        AfternoteEditorState(
            ui =
                AfternoteEditorUiHolder(
                    idState = TextFieldState(),
                    passwordState = TextFieldState(),
                    afternoteEditReceiverNameState = TextFieldState(),
                    phoneNumberState = TextFieldState(),
                    customServiceNameState = TextFieldState(),
                ),
            getCurrentForm = { EditorFormState() },
            updateForm = {},
        )

    private fun textFieldStateGetters() =
        AfternoteEditorUiHolder::class.java.methods
            .filter { it.parameterCount == 0 && it.returnType == TextFieldState::class.java }

    @Test
    fun `홀더의 모든 TextFieldState 는 지문에 반영되거나 제외 사유가 명시돼야 한다`() {
        val getters = textFieldStateGetters()
        assertTrue("TextFieldState 게터 열거 실패 — 리플렉션 전제 붕괴", getters.size >= 5)

        getters.forEach { getter ->
            val name = getter.name.removePrefix("get").replaceFirstChar { it.lowercase() }
            val state = newState()
            val form = EditorFormState()
            val before = editorContentSignature(form, state)
            (getter.invoke(state.ui) as TextFieldState).edit { replace(0, length, "완전성테스트입력") }
            val after = editorContentSignature(form, state)
            if (name in dialogTransientStates) {
                assertEquals("다이얼로그 휘발 입력 '$name' 은 지문에서 제외돼야 한다", before, after)
            } else {
                assertNotEquals(
                    "'$name' 입력이 지문에 반영되지 않는다 — editorContentSignature 에 추가하거나 " +
                        "dialogTransientStates 에 사유와 함께 올릴 것",
                    before,
                    after,
                )
            }
        }
    }

    @Test
    fun `메시지 블록 타이핑이 지문에 반영된다`() {
        val state = newState()
        val form = EditorFormState()
        val before = editorContentSignature(form, state)
        state.editorMessages
            .first()
            .titleState
            .edit { replace(0, length, "제목") }
        assertNotEquals(before, editorContentSignature(form, state))
    }

    @Test
    fun `폼 필드 변경이 지문에 반영된다 - 통째 직렬화 카나리`() {
        val state = newState()
        val before = editorContentSignature(EditorFormState(), state)
        val after = editorContentSignature(EditorFormState(pickedMemorialPhotoUri = "content://photo"), state)
        assertNotEquals(before, after)
    }

    @Test
    fun `서비스 선택이 지문에 반영된다 - 미선택(null)과 구분`() {
        val state = newState()
        val before = editorContentSignature(EditorFormState(), state)
        val after = editorContentSignature(EditorFormState(selectedService = "인스타그램"), state)
        assertNotEquals(before, after)
    }

    @Test
    fun `카테고리 구경만으로는 지문이 달라지지 않는다`() {
        val state = newState()
        val social = editorContentSignature(EditorFormState(), state)
        val gallery =
            editorContentSignature(
                EditorFormState(selectedCategory = EditorCategory.GALLERY),
                state,
            )
        assertEquals(social, gallery)
    }
}
