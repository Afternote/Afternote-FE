package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
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
                payload = payload,
            )

        assertEquals(AfternoteValidationError.ACCOUNT_CREDENTIALS_REQUIRED, error)
    }

    @Test
    fun `계정형 폼은 처리 방법이 없어도 저장할 수 있다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Social()),
                payload = payload.copy(accountId = "account", password = "password"),
            )

        assertNull(error)
    }

    @Test
    fun `갤러리 폼은 처리 방법이 없어도 저장할 수 있다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Gallery()),
                payload = payload,
            )

        assertNull(error)
    }

    @Test
    fun `필수 입력이 둘 이상 비면 복수 누락 오류를 반환한다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Social()),
                payload = payload.copy(serviceName = ""),
            )

        assertEquals(AfternoteValidationError.MULTIPLE_REQUIRED_FIELDS, error)
    }

    @Test
    fun `추모 폼은 플레이리스트가 비어 있어도 저장할 수 있다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Memorial()),
                payload = payload,
            )

        assertNull(error)
    }
}
