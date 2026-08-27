package com.afternote.feature.afternote.presentation.author.editor

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
    fun `계정형 폼은 ID와 비밀번호가 모두 필요하다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Social()),
                payload = payload.copy(processingMethods = listOf("계정 삭제")),
                selectedReceiverIds = listOf(1L),
            )

        assertEquals(AfternoteValidationError.ACCOUNT_CREDENTIALS_REQUIRED, error)
    }

    @Test
    fun `계정형 폼은 처리 방법이 한 개 이상 필요하다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Social()),
                payload = payload.copy(accountId = "account", password = "password"),
                selectedReceiverIds = listOf(1L),
            )

        assertEquals(AfternoteValidationError.ACTIONS_REQUIRED, error)
    }

    @Test
    fun `갤러리 폼은 처리 방법이 한 개 이상 필요하다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Gallery()),
                payload = payload,
                selectedReceiverIds = listOf(1L),
            )

        assertEquals(AfternoteValidationError.ACTIONS_REQUIRED, error)
    }

    @Test
    fun `추모 폼은 플레이리스트가 비어 있어도 저장할 수 있다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Memorial()),
                payload = payload,
                selectedReceiverIds = listOf(1L),
            )

        assertNull(error)
    }
}
