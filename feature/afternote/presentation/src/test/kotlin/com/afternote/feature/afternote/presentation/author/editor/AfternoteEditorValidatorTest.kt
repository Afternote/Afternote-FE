package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.EditorFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AfternoteEditorValidatorTest {
    private val payload =
        RegisterAfternotePayload(
            serviceName = "추억 노트",
            date = "2026.08.21",
        )

    @Test
    fun `추모 폼에 곡이 없으면 저장을 막는다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Memorial()),
                payload = payload,
                selectedReceiverIds = listOf(1L),
            )

        assertEquals(AfternoteValidationError.PLAYLIST_SONGS_REQUIRED, error)
    }

    @Test
    fun `추모 폼의 곡을 저장 검증 정본으로 사용한다`() {
        val form =
            EditorFormState(
                typeForm =
                    AfternoteTypeForm.Memorial(
                        playlistSongs =
                            listOf(
                                Song(
                                    selectionKey = "search:1",
                                    title = "노래",
                                    artist = "가수",
                                ),
                            ),
                    ),
            )

        val error =
            AfternoteEditorValidator.validate(
                form = form,
                payload = payload,
                selectedReceiverIds = listOf(1L),
            )

        assertNull(error)
    }
}
