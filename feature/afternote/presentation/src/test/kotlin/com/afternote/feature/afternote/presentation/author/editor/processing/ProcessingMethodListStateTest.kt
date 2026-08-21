package com.afternote.feature.afternote.presentation.author.editor.processing

import org.junit.Assert.assertFalse
import org.junit.Test

class ProcessingMethodListStateTest {
    @Test
    fun `입력 필드를 닫으면 표시 상태가 false가 된다`() {
        val state = ProcessingMethodListState(initialShowTextField = true)

        state.hideTextField()

        assertFalse(state.showTextField)
    }
}
