package com.afternote.feature.mindrecord.data.repositoryimpl

import com.afternote.core.network.model.ApiException
import com.afternote.feature.mindrecord.domain.error.DeliveryNotReadyException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 수신자 조회 실패의 계층 경계 가드 (#614).
 *
 * "서버 에러 코드" 는 전송 계층 개념이라 화면까지 올라가면 안 된다. 여기서 도메인 예외로
 * 바꿔 두면 presentation 은 타입만 보고 분기하고 `core:network` 를 모른다.
 */
class MindRecordReceiverFailureMapperTest {
    private fun apiFailure(
        status: Int,
        code: Int,
    ) = Result.failure<Unit>(
        ApiException(status = status, code = code, serverMessage = "서버 원문", message = "서버 원문"),
    )

    @Test
    fun `전달 조건 미충족 코드는 도메인 예외가 된다`() {
        val mapped = apiFailure(status = 403, code = 2009).mapReceiverFailure()

        assertTrue(mapped.exceptionOrNull() is DeliveryNotReadyException)
    }

    @Test
    fun `원래 예외를 cause 로 남긴다`() {
        // 진단 정보를 버리지 않는다 — 화면에 쓰지 않을 뿐이다.
        val original = apiFailure(status = 403, code = 2009)
        val mapped = original.mapReceiverFailure()

        assertSame(original.exceptionOrNull(), mapped.exceptionOrNull()?.cause)
    }

    @Test
    fun `다른 서버 코드는 그대로 흘려보낸다`() {
        val original = apiFailure(status = 500, code = 1004)

        assertSame(original.exceptionOrNull(), original.mapReceiverFailure().exceptionOrNull())
    }

    @Test
    fun `서버 응답이 없는 실패는 손대지 않는다`() {
        val original = Result.failure<Unit>(IOException("timeout"))

        assertSame(original.exceptionOrNull(), original.mapReceiverFailure().exceptionOrNull())
    }

    @Test
    fun `성공은 그대로 통과한다`() {
        assertSame(Unit, Result.success(Unit).mapReceiverFailure().getOrNull())
    }
}
