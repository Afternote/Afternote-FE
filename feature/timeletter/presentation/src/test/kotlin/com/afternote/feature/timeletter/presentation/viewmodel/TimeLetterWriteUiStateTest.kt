package com.afternote.feature.timeletter.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeLetterWriteUiStateTest {
    @Test
    fun `updating title preserves draft text contents`() {
        val state = TimeLetterWriteUiState(draftTextContents = mapOf(1L to "content"))

        val updated = state.withDraftTitle("title")

        assertEquals("title", updated.draftTitle)
        assertEquals(mapOf(1L to "content"), updated.draftTextContents)
    }

    @Test
    fun `updating text content preserves title and other blocks`() {
        val state =
            TimeLetterWriteUiState(
                draftTitle = "title",
                draftTextContents = mapOf(1L to "first"),
            )

        val updated = state.withDraftTextContent(blockId = 2L, content = "second")

        assertEquals("title", updated.draftTitle)
        assertEquals(mapOf(1L to "first", 2L to "second"), updated.draftTextContents)
    }

    @Test
    fun `updating content before navigation merges visible and offscreen blocks`() {
        val state =
            TimeLetterWriteUiState(
                draftTitle = "previous title",
                draftTextContents = mapOf(1L to "offscreen", 2L to "previous visible"),
            )

        val updated =
            state.withDraftContent(
                title = "latest title",
                textContents = mapOf(2L to "latest visible"),
            )

        assertEquals("latest title", updated.draftTitle)
        assertEquals(
            mapOf(1L to "offscreen", 2L to "latest visible"),
            updated.draftTextContents,
        )
    }
}
