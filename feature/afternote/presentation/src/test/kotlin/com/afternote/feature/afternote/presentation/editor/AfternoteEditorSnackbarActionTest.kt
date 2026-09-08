package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError.Upload.Target
import com.afternote.feature.afternote.presentation.editor.state.AfternoteValidationError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 스낵바 «다시 시도» 액션은 추모 영상 썸네일 실패 두 갈래에만 붙는다 (#1550).
 *
 * «재시도 가능» 을 별도 불리언으로 들면 그 스낵바가 닫힌 뒤에도 상태가 남아, 다음에 뜨는 저장 실패
 * 스낵바에 썸네일 재시도가 붙어 저장 재시도로 오인된다 — 판정은 지금 뜨는 오류 자체로 한다.
 */
class AfternoteEditorSnackbarActionTest {
    @Test
    fun `썸네일 업로드 실패와 추출 실패에는 재시도 액션을 건다`() {
        assertTrue(AfternoteEditorError.Upload(Target.THUMBNAIL).offersMemorialThumbnailRetry())
        assertTrue(AfternoteEditorError.Upload(Target.THUMBNAIL_EXTRACT).offersMemorialThumbnailRetry())
    }

    @Test
    fun `저장 실패와 그 밖의 오류에는 썸네일 재시도가 붙지 않는다`() {
        listOf(
            AfternoteEditorError.Upload(Target.SAVE_MEDIA),
            AfternoteEditorError.Network,
            AfternoteEditorError.Server,
            AfternoteEditorError.ReceiverSelectionUnavailable,
            AfternoteEditorError.Validation(AfternoteValidationError.TITLE_REQUIRED),
        ).forEach { error ->
            assertFalse("$error 에 썸네일 재시도가 붙으면 저장 재시도로 오인된다", error.offersMemorialThumbnailRetry())
        }
    }
}
