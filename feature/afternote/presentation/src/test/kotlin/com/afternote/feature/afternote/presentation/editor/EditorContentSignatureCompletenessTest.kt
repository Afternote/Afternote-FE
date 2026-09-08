package com.afternote.feature.afternote.presentation.editor

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.MemorialVideoAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 내부 지문 대신 실제 화면의 뒤로가기·이탈 경고 계약으로 입력 누락과 오탐을 검증한다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EditorContentSignatureCompletenessTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val activeCase = mutableStateOf<ExitCase?>(null)
    private val transientUiStates = setOf("serviceSearchQueryState")

    @Before
    fun setUp() {
        composeRule.setContent {
            activeCase.value?.let { case ->
                key(case) {
                    AfternoteTheme {
                        AfternoteEditorScreen(
                            form = case.form.value,
                            state = case.state,
                            onBackClick = { case.backCalls++ },
                            onRegisterClick = {},
                            snackbarMessage = null,
                            onSnackbarMessageConsumed = {},
                            validationMessage = null,
                            onValidationMessageConsumed = {},
                            content = {},
                        )
                    }
                }
            }
        }
    }

    private class ExitCase(
        initialForm: EditorFormState,
        val state: AfternoteEditorState,
    ) {
        val form = mutableStateOf(initialForm)
        var backCalls = 0
    }

    private fun assertExitGuard(
        before: EditorFormState = EditorFormState(),
        after: EditorFormState = before,
        changed: Boolean = true,
        prepare: (AfternoteEditorState) -> Unit = {},
        edit: (AfternoteEditorState) -> Unit = {},
    ) {
        val state = newState().also(prepare)
        val case = ExitCase(before, state)
        composeRule.runOnIdle { activeCase.value = case }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            case.form.value = after
            edit(state)
        }
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        val warning = composeRule.onNodeWithText("작성 중인 내용이 사라집니다.", substring = true)
        if (changed) warning.assertExists() else warning.assertDoesNotExist()
        assertEquals(if (changed) 0 else 1, case.backCalls)
    }

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

    @Test
    fun `모든 TextFieldState 편집은 이탈 경고에 반영되거나 제외 사유가 있어야 한다`() {
        val getters =
            AfternoteEditorState::class.java.methods
                .filter { it.parameterCount == 0 && it.returnType == TextFieldState::class.java }
        assertTrue(getters.size >= 3)
        getters.forEach { getter ->
            val name = getter.name.removePrefix("get").replaceFirstChar { it.lowercase() }
            assertExitGuard(changed = name !in transientUiStates) { state ->
                (getter.invoke(state) as TextFieldState).edit { replace(0, length, "완전성테스트입력") }
            }
        }
    }

    @Test
    fun `메시지 블록 입력 후 이탈을 경고한다`() {
        assertExitGuard(prepare = { it.addEditorMessage() }) { state ->
            state.editorMessages
                .first()
                .titleState
                .edit { replace(0, length, "제목") }
        }
    }

    @Test
    fun `수신자와 카테고리 전용 입력 변경 후 이탈을 경고한다`() {
        assertExitGuard(after = EditorFormState(afternoteEditReceivers = listOf(AfternoteEditorReceiver(1L, "수신자", "딸"))))
        assertExitGuard(before = memorial(), after = memorial(photo = "content://photo"))
        assertExitGuard(after = EditorFormState(typeForm = AfternoteTypeForm.Social(selectedService = "인스타그램")))
    }

    @Test
    fun `입력 없는 카테고리 구경은 경고하지 않는다`() {
        AfternoteType.entries.forEach { type ->
            assertExitGuard(after = EditorFormState(typeForm = AfternoteTypeForm.pristineFor(type)), changed = false)
        }
    }

    @Test
    fun `카테고리 전환으로 입력을 버리면 경고한다`() {
        assertExitGuard(
            before = EditorFormState(typeForm = AfternoteTypeForm.Social(selectedService = "인스타그램")),
            after = EditorFormState(typeForm = AfternoteTypeForm.Gallery()),
        )
    }

    @Test
    fun `선택 영상은 경고하고 자동 파생 썸네일만 바뀌면 경고하지 않는다`() {
        val video = EditableMemorialVideo.empty().withSelection("content://video")
        assertExitGuard(before = memorial(), after = memorial(video))
        assertExitGuard(
            before = memorial(video),
            after = memorial(video.withSelectionThumbnail("https://cdn.test/thumb.jpg")),
            changed = false,
        )
    }

    @Test
    fun `서버 영상 삭제는 경고하고 서버 썸네일만 바뀌면 경고하지 않는다`() {
        val attachment = MemorialVideoAttachment("https://cdn.test/video.mp4", "https://cdn.test/thumb.jpg")
        val video = EditableMemorialVideo.fromPersisted(attachment)
        assertExitGuard(before = memorial(video), after = memorial())
        assertExitGuard(
            before = memorial(video),
            after = memorial(EditableMemorialVideo.fromPersisted(attachment.copy(thumbnailUrl = "https://cdn.test/other.jpg"))),
            changed = false,
        )
    }

    private fun memorial(
        video: EditableMemorialVideo = EditableMemorialVideo.empty(),
        photo: String? = null,
    ) = EditorFormState(typeForm = AfternoteTypeForm.Memorial(video = video, pickedPhotoUri = photo))
}
