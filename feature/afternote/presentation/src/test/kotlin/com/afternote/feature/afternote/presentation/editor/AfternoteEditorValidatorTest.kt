package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.presentation.editor.message.EditorMessageTextBlock
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

    // ---- 임시저장(asDraft=true)이 «건너뛰는 것» 과 «그대로 막는 것» (#808) ----

    @Test
    fun `임시저장은 ESTATE 를 그대로 막는다 - 통과시키면 매퍼가 죽는다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Estate),
                payload = payload,
                asDraft = true,
            )

        assertEquals(AfternoteValidationError.UNIMPLEMENTED_TYPE, error)
    }

    @Test
    fun `임시저장은 제목을 그대로 요구한다 - 서버가 NotBlank 로 400 을 낸다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Gallery()),
                payload = payload.copy(serviceName = "  "),
                asDraft = true,
            )

        assertEquals(AfternoteValidationError.TITLE_REQUIRED, error)
    }

    @Test
    fun `임시저장은 제목만 채운 남기실 말씀 블록을 그대로 막는다`() {
        val error =
            AfternoteEditorValidator.validate(
                form = EditorFormState(typeForm = AfternoteTypeForm.Gallery()),
                payload = payload.copy(messageBlocks = listOf(EditorMessageTextBlock(title = "제목", body = ""))),
                asDraft = true,
            )

        assertEquals(AfternoteValidationError.LEAVE_MESSAGE_BODY_REQUIRED, error)
    }

    @Test
    fun `임시저장은 계정 정보만 건너뛴다 - 서버가 완화하는 축이다`() {
        val form = EditorFormState(typeForm = AfternoteTypeForm.Social())

        assertEquals(
            AfternoteValidationError.ACCOUNT_CREDENTIALS_REQUIRED,
            AfternoteEditorValidator.validate(form = form, payload = payload),
        )
        assertNull(AfternoteEditorValidator.validate(form = form, payload = payload, asDraft = true))
    }
}
