package com.afternote.core.data.repoimpl

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.error.FileUploadFailure
import com.afternote.core.network.model.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

/**
 * 파일 업로드 실패 번역 계약 가드 (#1868).
 *
 * 계약 — 서버가 파일 크기 초과(code 1803)로 거절하면 [FileUploadFailure.FileSizeExceeded] 로
 * 번역해 소비처가 타입으로 분기할 수 있게 하고, 사유를 확인하지 못한 실패는 원본 그대로 흘려보내
 * 기존 재시도 안내 경로에 남긴다. 사진·영상 두 구현이 같은 함수를 체인하므로 여기서 한 번 고정한다.
 */
class UploadFailureTranslationTest {
    @Test
    fun `파일 크기 초과(1803)는 FileSizeExceeded 로 번역`() {
        val cause = apiException(code = 1803, serverMessage = "파일 크기는 10MB 이하여야 합니다.")

        val result = Result.failure<Unit>(cause).mapUploadFailure()

        val error = result.exceptionOrNull()
        assertTrue(error is FileUploadFailure.FileSizeExceeded)
        assertSame("서버 코드값은 cause 인 ApiException 이 갖는다", cause, error?.cause)
    }

    @Test
    fun `그 외 code 실패는 원본 ApiException 유지`() {
        val cause = apiException(code = 1400, serverMessage = "요청 값이 올바르지 않습니다.")

        val result = Result.failure<Unit>(cause).mapUploadFailure()

        val error = result.exceptionOrNull()
        assertTrue(error is ApiException)
        assertEquals(1400, (error as ApiException).code)
    }

    @Test
    fun `ApiException 이 아닌 실패는 원본 유지 (S3 PUT 실패 등 재시도가 통하는 경로)`() {
        val cause = IllegalStateException("S3 upload failed: 503 Service Unavailable")

        val result = Result.failure<Unit>(cause).mapUploadFailure()

        assertSame(cause, result.exceptionOrNull())
    }

    @Test
    fun `취소는 Result 로 소비하지 않고 다시 던짐 (코루틴 취소 보존)`() {
        val thrown =
            try {
                runCatchingCancellable<Unit> { throw CancellationException("업로드 취소") }.mapUploadFailure()
                null
            } catch (expected: CancellationException) {
                expected
            }

        assertTrue(thrown is CancellationException)
    }

    @Test
    fun `성공은 그대로 통과`() {
        val result = Result.success("uploaded").mapUploadFailure()

        assertEquals("uploaded", result.getOrNull())
    }

    private fun apiException(
        code: Int,
        serverMessage: String,
    ) = ApiException(
        status = 400,
        code = code,
        serverMessage = serverMessage,
        fallbackMessage = "요청에 실패했습니다.",
    )
}
