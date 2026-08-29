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
    fun `서버가 응답한 실패는 그대로 흘려보낸다`() {
        val original =
            Result.failure<Unit>(
                ApiException(status = 400, code = 400, serverMessage = null, fallbackMessage = "x"),
            )

        assertSame(original.exceptionOrNull(), original.mapAuthoringFailure().exceptionOrNull())
    }

    @Test
    fun `전송 계층 IO 실패는 NetworkUnavailable 로 치환한다`() {
        val original = IOException("timeout")

        val mapped = Result.failure<Unit>(original).mapAuthoringFailure()

        assertTrue(mapped.exceptionOrNull() is AfternoteFailure.NetworkUnavailable)
    }

    @Test
    fun `원래 예외를 cause 로 남긴다`() {
        // 진단 정보를 버리지 않는다 — 화면에 쓰지 않을 뿐이다.
        val original = IOException("timeout")

        val mapped = Result.failure<Unit>(original).mapAuthoringFailure()

        assertSame(original, mapped.exceptionOrNull()?.cause)
    }

    @Test
    fun `서버·전송 어느 쪽도 아닌 예외는 손대지 않는다`() {
        val original = Result.failure<Unit>(IllegalStateException("boom"))

        assertSame(original.exceptionOrNull(), original.mapAuthoringFailure().exceptionOrNull())
    }

    @Test
    fun `성공은 그대로 통과한다`() {
        assertSame(Unit, Result.success(Unit).mapAuthoringFailure().getOrNull())
    }
}
