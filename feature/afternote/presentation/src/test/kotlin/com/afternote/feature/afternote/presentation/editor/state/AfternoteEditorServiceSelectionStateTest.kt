package com.afternote.feature.afternote.presentation.editor.state

import androidx.compose.foundation.text.input.TextFieldState
import com.afternote.feature.afternote.domain.AfternoteType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AfternoteEditorServiceSelectionStateTest {
    @Test
    fun `선택은 정확한 display key를 저장한 뒤 sheet와 query를 닫는다`() {
        var selectedService: String? = null
        val state = editorState(setService = { selectedService = it })
        state.openServiceSelectionSheet()
        state.serviceSearchQueryState.edit { append("  FACE  ") }

        state.onServiceSelected("페이스북")

        assertEquals("페이스북", selectedService)
        assertFalse(state.isServiceSelectionSheetVisible)
        assertTrue(state.serviceSearchQueryState.text.isEmpty())
    }

    @Test
    fun `dismiss는 기존 선택을 바꾸지 않고 query만 초기화한다`() {
        var selectedService: String? = null
        val state = editorState(setService = { selectedService = it })
        state.openServiceSelectionSheet()
        state.serviceSearchQueryState.edit { append("인스타") }

        state.dismissServiceSelectionSheet()

        assertNull(selectedService)
        assertFalse(state.isServiceSelectionSheetVisible)
        assertTrue(state.serviceSearchQueryState.text.isEmpty())
    }

    @Test
    fun `카테고리 변경도 열린 sheet와 query를 정리한 뒤 타입을 전달한다`() {
        var selectedType: AfternoteType? = null
        val state = editorState(setType = { selectedType = it })
        state.openServiceSelectionSheet()
        state.serviceSearchQueryState.edit { append("갤러리") }

        state.onTypeSelected(AfternoteType.GALLERY_AND_FILES)

        assertEquals(AfternoteType.GALLERY_AND_FILES, selectedType)
        assertFalse(state.isServiceSelectionSheetVisible)
        assertTrue(state.serviceSearchQueryState.text.isEmpty())
    }

    private fun editorState(
        setType: (AfternoteType) -> Unit = {},
        setService: (String) -> Unit = {},
    ): AfternoteEditorState =
        AfternoteEditorState(
            idState = TextFieldState(),
            passwordState = TextFieldState(),
            serviceSearchQueryState = TextFieldState(),
            getCurrentForm = { EditorFormState() },
            setType = setType,
            setService = setService,
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
}
