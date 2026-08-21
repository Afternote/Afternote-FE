package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.core.domain.error.NetworkUnavailableException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationKind
import com.afternote.feature.afternote.domain.usecase.editor.MemorialPhotoSaveException
import com.afternote.feature.afternote.domain.usecase.editor.MemorialVideoSaveException
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationError
import com.afternote.feature.afternote.presentation.author.editor.state.AfternoteValidationException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class AfternoteEditorErrorTest {
    @Test
    fun `로컬 검증 실패는 Validation으로 보존`() {
        val failure = AfternoteValidationException(AfternoteValidationError.TITLE_REQUIRED)

        assertEquals(
            AfternoteEditorError.Validation(AfternoteValidationError.TITLE_REQUIRED),
            failure.toAfternoteEditorError(),
        )
    }

    @Test
    fun `서버 검증 실패는 Validation으로 변환`() {
        val failure = AfternoteAuthoringValidationException(AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED)

        assertEquals(
            AfternoteEditorError.Validation(AfternoteValidationError.RECEIVERS_REQUIRED),
            failure.toAfternoteEditorError(),
        )
    }

    @Test
    fun `네트워크 단절은 Network로 변환`() {
        val failure = NetworkUnavailableException(IOException("timeout"))

        assertEquals(AfternoteEditorError.Network, failure.toAfternoteEditorError())
    }

    @Test
    fun `저장 중 영상 업로드 실패는 SAVE_MEDIA Upload로 변환`() {
        val failure = MemorialVideoSaveException(IOException("upload failed"))

        assertEquals(
            AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA),
            failure.toAfternoteEditorError(),
        )
    }

    @Test
    fun `저장 중 사진 업로드 실패는 SAVE_MEDIA Upload로 변환`() {
        val failure = MemorialPhotoSaveException(IOException("upload failed"))

        assertEquals(
            AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA),
            failure.toAfternoteEditorError(),
        )
    }

    @Test
    fun `그 밖의 저장 실패는 Server로 변환`() {
        assertEquals(
            AfternoteEditorError.Server,
            IllegalStateException("failed").toAfternoteEditorError(),
        )
    }
}
