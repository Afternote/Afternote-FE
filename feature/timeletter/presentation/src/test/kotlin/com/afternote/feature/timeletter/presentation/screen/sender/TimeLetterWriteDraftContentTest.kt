package com.afternote.feature.timeletter.presentation.screen.sender

import com.afternote.feature.timeletter.presentation.viewmodel.EditorBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeLetterWriteDraftContentTest {
    @Test
    fun `collect text contents keeps draft for blocks outside composition`() {
        val contents =
            collectTextBlockContents(
                editorBlocks = listOf(EditorBlock.Text(1L), EditorBlock.Text(2L)),
                visibleTextContents = mapOf(2L to "visible content"),
                draftTextContents = mapOf(1L to "offscreen content", 2L to "previous content"),
            )

        assertEquals(
            mapOf(1L to "offscreen content", 2L to "visible content"),
            contents,
        )
    }

    @Test
    fun `collect text contents excludes non text blocks`() {
        val contents =
            collectTextBlockContents(
                editorBlocks =
                    listOf(
                        EditorBlock.Text(1L),
                        EditorBlock.Link(2L, "https://example.com"),
                    ),
                visibleTextContents = emptyMap(),
                draftTextContents = mapOf(1L to "draft", 2L to "not text"),
            )

        assertEquals(mapOf(1L to "draft"), contents)
    }
}
