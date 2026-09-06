package com.afternote.feature.afternote.presentation.editor

import androidx.compose.foundation.text.input.TextFieldState
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.MemorialVideoAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 이탈 가드 상태 지문([editorContentSignature])의 완전성 계약 테스트.
 *
 * [AfternoteEditorState]의 [TextFieldState]를 리플렉션으로 전수 열거한 뒤 하나씩 실제로
 * 입력해 보고 지문이 반응하는지 검사한다. 상태 홀더에 입력 상태가 새로 생기면 이 테스트가 자동으로
 * 그 필드를 집어 들므로, 지문에 반영하거나 [dialogTransientStates] 에 사유와 함께 올리는 결정이
 * 강제된다. 폼 쪽은 공용 필드가 통째 직렬화라 필드 추가가 자동 포함이고, 카테고리 축은
 * [AfternoteTypeForm.pristineFor] 의 exhaustive `when` 이 컴파일 단계에서 누락을 잡는다 — 양쪽에 카나리만 둔다.
 */
class EditorContentSignatureCompletenessTest {
    /** 서비스 시트를 닫거나 선택하면 비우는 검색 query라 이탈 지문에서 제외한다. */
    private val transientUiStates =
        setOf("serviceSearchQueryState")

    private fun newState(): AfternoteEditorState =
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
            setMemorialAudio = {},
            removeMemorialAudio = {},
            addReceiverIfAbsent = { _, _, _ -> },
            applyPrefill = {},
            setMemorialThumbnail = {},
            deleteReceiver = {},
            replaceReceiversIfEmpty = {},
            addProcessingMethod = {},
            deleteProcessingMethod = {},
            editProcessingMethod = { _, _ -> },
        )

    private fun textFieldStateGetters() =
        AfternoteEditorState::class.java.methods
            .filter { it.parameterCount == 0 && it.returnType == TextFieldState::class.java }

    @Test
    fun `에디터 상태의 모든 TextFieldState 는 지문에 반영되거나 제외 사유가 명시돼야 한다`() {
        val getters = textFieldStateGetters()
        assertTrue("TextFieldState 게터 열거 실패 — 리플렉션 전제 붕괴", getters.size >= 3)

        getters.forEach { getter ->
            val name = getter.name.removePrefix("get").replaceFirstChar { it.lowercase() }
            val state = newState()
            val form = EditorFormState()
            val before = editorContentSignature(form, state)
            (getter.invoke(state) as TextFieldState).edit { replace(0, length, "완전성테스트입력") }
            val after = editorContentSignature(form, state)
            if (name in transientUiStates) {
                assertEquals("휘발 UI 입력 '$name' 은 지문에서 제외돼야 한다", before, after)
            } else {
                assertNotEquals(
                    "'$name' 입력이 지문에 반영되지 않는다 — editorContentSignature 에 추가하거나 " +
                        "transientUiStates 에 사유와 함께 올릴 것",
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
        state.addEditorMessage()
        val before = editorContentSignature(form, state)
        state.editorMessages
            .first()
            .titleState
            .edit { replace(0, length, "제목") }
        assertNotEquals(before, editorContentSignature(form, state))
    }

    @Test
    fun `공용 폼 필드 변경이 지문에 반영된다 - 통째 직렬화 카나리`() {
        val state = newState()
        val before = editorContentSignature(EditorFormState(), state)
        val after =
            editorContentSignature(
                EditorFormState(
                    afternoteEditReceivers = listOf(AfternoteEditorReceiver(id = 1L, name = "김수신", label = "딸")),
                ),
                state,
            )
        assertNotEquals(before, after)
    }

    @Test
    fun `카테고리 전용 입력 변경이 지문에 반영된다 - 같은 카테고리끼리 비교`() {
        val state = newState()
        val before = editorContentSignature(EditorFormState(typeForm = AfternoteTypeForm.Memorial()), state)
        val after =
            editorContentSignature(
                EditorFormState(typeForm = AfternoteTypeForm.Memorial(pickedPhotoUri = "content://photo")),
                state,
            )
        assertNotEquals(before, after)
    }

    @Test
    fun `서비스 선택이 지문에 반영된다 - 미선택(null)과 구분`() {
        val state = newState()
        val before = editorContentSignature(EditorFormState(typeForm = AfternoteTypeForm.Social()), state)
        val after =
            editorContentSignature(
                EditorFormState(typeForm = AfternoteTypeForm.Social(selectedService = "인스타그램")),
                state,
            )
        assertNotEquals(before, after)
    }

    @Test
    fun `어느 카테고리를 골라도 입력이 없으면 지문이 같다`() {
        val state = newState()
        val signatures =
            AfternoteType.entries.map { type ->
                editorContentSignature(
                    EditorFormState(typeForm = AfternoteTypeForm.pristineFor(type)),
                    state,
                )
            }
        assertEquals(
            "카테고리 구경은 변경이 아니다 — 입력이 빈 카테고리는 전부 같은 지문이어야 한다",
            1,
            signatures.distinct().size,
        )
    }

    @Test
    fun `카테고리 전환으로 입력값이 버려지면 지문이 달라진다`() {
        val state = newState()
        val filled =
            editorContentSignature(
                EditorFormState(typeForm = AfternoteTypeForm.Social(selectedService = "인스타그램")),
                state,
            )
        val switched = editorContentSignature(EditorFormState(typeForm = AfternoteTypeForm.Gallery()), state)
        assertNotEquals(filled, switched)
    }

    @Test
    fun `자동 파생 썸네일은 지문에서 제외되고 영상 자체는 반영된다`() {
        val state = newState()
        val picked = MemorialVideoAttachment(url = "content://video")
        val pristine = editorContentSignature(EditorFormState(typeForm = AfternoteTypeForm.Memorial()), state)
        val withVideo =
            editorContentSignature(
                EditorFormState(
                    typeForm =
                        AfternoteTypeForm.Memorial(
                            video = EditableMemorialVideo.empty().withSelection(picked.url),
                        ),
                ),
                state,
            )

        assertNotEquals("고른 영상 자체는 사용자 입력이다", pristine, withVideo)
        assertEquals(
            "썸네일은 영상에서 파생된 값이라 사용자 입력이 아니다 — 복원 뒤 재추출로 URL 이 바뀌어도 지문은 그대로여야 한다",
            withVideo,
            editorContentSignature(
                EditorFormState(
                    typeForm =
                        AfternoteTypeForm.Memorial(
                            video =
                                EditableMemorialVideo
                                    .empty()
                                    .withSelection(picked.url)
                                    .withSelectionThumbnail("https://cdn.test/thumb.jpg"),
                        ),
                ),
                state,
            ),
        )
    }

    /**
     * #1561 은 「prefill 이 채운 서버 원본은 지문에서 제외된다」 는 카나리로 «사용자 입력만 센다» 를 잠갔다.
     * #1597 이 서버 원본 삭제를 열면서 그 존재 자체가 사용자가 바꿀 수 있는 사실이 됐으므로 지문은 수정
     * 진입 기준선과 비교해 서버 원본을 포함한다. 저장 성공 후 서버 값만 새로고침하는 경로가 생기면 그때
     * 기준선을 다시 잡아야 한다 — 제외로 되돌리면 서버 삭제가 이탈 경고 없이 사라진다.
     */
    @Test
    fun `서버 영상 삭제는 지문에 반영되고 서버 썸네일만 달라지면 제외된다`() {
        val state = newState()
        val serverVideo =
            MemorialVideoAttachment(
                url = "https://cdn.test/farewell.mp4",
                thumbnailUrl = "https://cdn.test/farewell-thumb.jpg",
            )
        val withServerVideo =
            editorContentSignature(
                EditorFormState(typeForm = AfternoteTypeForm.Memorial(video = EditableMemorialVideo.fromPersisted(serverVideo))),
                state,
            )

        assertNotEquals(
            "서버 원본 삭제는 저장하지 않고 이탈할 때 경고해야 한다",
            withServerVideo,
            editorContentSignature(EditorFormState(typeForm = AfternoteTypeForm.Memorial()), state),
        )
        assertEquals(
            "서버 썸네일은 영상에서 파생된 값이라 지문에서 제외한다",
            withServerVideo,
            editorContentSignature(
                EditorFormState(
                    typeForm =
                        AfternoteTypeForm.Memorial(
                            video =
                                EditableMemorialVideo.fromPersisted(
                                    serverVideo.copy(thumbnailUrl = "https://cdn.test/another-thumb.jpg"),
                                ),
                        ),
                ),
                state,
            ),
        )
    }
}
