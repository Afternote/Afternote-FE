package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.error.AfternoteFailure
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class AfternoteEditorErrorTest {
    @Test
    fun `네트워크 단절은 Network로 변환`() {
        val failure = AfternoteFailure.NetworkUnavailable(IOException("timeout"))

        assertEquals(AfternoteEditorError.Network, failure.toAfternoteEditorError())
    }

    @Test
    fun `저장 중 영상 업로드 실패는 SAVE_MEDIA Upload로 변환`() {
        val failure = AfternoteFailure.MediaSave(MediaKind.VIDEO, IOException("upload failed"))

        assertEquals(
            AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA),
            failure.toAfternoteEditorError(),
        )
    }

    @Test
    fun `저장 중 사진 업로드 실패는 SAVE_MEDIA Upload로 변환`() {
        val failure = AfternoteFailure.MediaSave(MediaKind.PHOTO, IOException("upload failed"))

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
