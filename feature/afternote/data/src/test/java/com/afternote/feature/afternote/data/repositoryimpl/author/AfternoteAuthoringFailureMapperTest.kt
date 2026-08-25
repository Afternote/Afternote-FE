package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.network.model.ApiException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationKind
import com.afternote.feature.afternote.domain.error.AfternoteFailure
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * [mapAuthoringFailure] 회귀 가드.
 * 저장 API 실패 Throwable 중 "수신자 최소 1명 필요(서버 코드 475)" 만 도메인 검증 예외로 치환하고,
 * 나머지는 원본 인스턴스를 그대로 통과시키는지 검증한다. 475 는 두 경로(ApiException·HttpException 바디)
 * 로 들어올 수 있어 둘 다 커버한다.
 */
class AfternoteAuthoringFailureMapperTest {
    @Test
    fun `이미 검증 예외면 그대로 반환`() {
        val original = AfternoteFailure.AuthoringValidation(AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED)
        assertSame(original, mapAuthoringFailure(original))
    }

    @Test
    fun `ApiException 475면 RECEIVERS_REQUIRED 검증 예외로 치환`() {
        val result = mapAuthoringFailure(ApiException(status = 400, code = 475, serverMessage = null, message = "x"))
        assertTrue(result is AfternoteFailure.AuthoringValidation)
        assertEquals(
            AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED,
            (result as AfternoteFailure.AuthoringValidation).kind,
        )
    }

    @Test
    fun `ApiException 다른 코드는 그대로 통과`() {
        val original = ApiException(status = 400, code = 400, serverMessage = null, message = "x")
        assertSame(original, mapAuthoringFailure(original))
    }

    @Test
    fun `일반 예외는 그대로 통과`() {
        val original = IllegalStateException("boom")
        assertSame(original, mapAuthoringFailure(original))
    }

    @Test
    fun `HttpException 400 + 바디 code 475면 검증 예외로 치환`() {
        val result = mapAuthoringFailure(httpException(code = 400, body = """{"code":475}"""))
        assertTrue(result is AfternoteFailure.AuthoringValidation)
    }

    @Test
    fun `HttpException 400 + 바디 code가 475가 아니면 통과`() {
        val original = httpException(code = 400, body = """{"code":999}""")
        assertSame(original, mapAuthoringFailure(original))
    }

    @Test
    fun `HttpException 비400은 통과`() {
        val original = httpException(code = 500, body = """{"code":475}""")
        assertSame(original, mapAuthoringFailure(original))
    }

    private fun httpException(
        code: Int,
        body: String,
    ): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaTypeOrNull())
        return HttpException(Response.error<Any>(code, responseBody))
    }
}
