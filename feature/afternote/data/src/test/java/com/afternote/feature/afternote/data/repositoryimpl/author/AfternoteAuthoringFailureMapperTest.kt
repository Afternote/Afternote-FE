package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.network.model.ApiException
import com.afternote.feature.afternote.domain.error.AfternoteFailure
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * [mapAuthoringFailure] 회귀 가드. 전송 계층 실패만 도메인 예외로 치환하고,
 * 서버가 응답한 실패는 원본 인스턴스를 유지하는지 검증한다.
 */
class AfternoteAuthoringFailureMapperTest {
    @Test
    fun `서버가 응답한 ApiException은 그대로 통과`() {
        val original = ApiException(status = 400, code = 400, serverMessage = null, fallbackMessage = "x")
        assertSame(original, mapAuthoringFailure(original))
    }

    @Test
    fun `전송 계층 IO 실패는 AfternoteFailure_NetworkUnavailable 로 치환`() {
        val original = IOException("timeout")

        val result = mapAuthoringFailure(original)

        assertTrue(result is AfternoteFailure.NetworkUnavailable)
        assertSame(original, result.cause)
    }

    @Test
    fun `일반 예외는 그대로 통과`() {
        val original = IllegalStateException("boom")
        assertSame(original, mapAuthoringFailure(original))
    }
}
