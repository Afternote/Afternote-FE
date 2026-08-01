package com.afternote.core.common.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * [ErrorReporter] 창구가 태우는 정책 검증 — 취소 제외, 예외 원문 제거.
 *
 * 구현(Crashlytics)이 아니라 인터페이스 기본 구현이 검증 대상이라 fake 로 확인한다.
 */
class ErrorReporterPolicyTest {
    private class FakeErrorReporter : ErrorReporter {
        val written = mutableListOf<Pair<Throwable, Map<String, String>>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            written += throwable to attributes
        }
    }

    /** 서버 응답 본문·자격증명 조각이 담길 수 있는 예외를 흉내낸다. */
    private class ServerFailure(
        message: String,
        cause: Throwable? = null,
    ) : RuntimeException(message, cause)

    @Test
    fun `코루틴 취소는 기록하지 않는다`() {
        val reporter = FakeErrorReporter()

        reporter.recordFailure(CancellationException("스코프 취소"))

        assertTrue("취소는 리포팅 백엔드에 도달하면 안 된다", reporter.written.isEmpty())
    }

    @Test
    fun `예외 문구는 리포팅 백엔드로 넘어가지 않는다`() {
        val reporter = FakeErrorReporter()
        val secret = "user@example.com 계정의 토큰 abc123 이 유효하지 않습니다"

        reporter.recordFailure(ServerFailure(secret))

        val (recorded, _) = reporter.written.single()
        assertEquals("문구 자리에는 타입 이름만 남는다", ServerFailure::class.java.name, recorded.message)
        assertTrue("원문이 어디에도 남으면 안 된다", secret !in recorded.toString())
    }

    @Test
    fun `원인 예외의 문구도 넘어가지 않는다`() {
        val reporter = FakeErrorReporter()
        val causeSecret = "Set-Cookie: session=deadbeef"

        reporter.recordFailure(ServerFailure("래핑된 실패", IOException(causeSecret)))

        val (recorded, _) = reporter.written.single()
        assertNull("cause 는 잇지 않는다 — 원인 문구도 같은 위험이다", recorded.cause)
        assertTrue(causeSecret !in recorded.toString())
    }

    @Test
    fun `버려진 문구 대신 타입 정보를 속성으로 남긴다`() {
        val reporter = FakeErrorReporter()

        reporter.recordFailure(ServerFailure("문구", IOException("원인")))

        val (_, attributes) = reporter.written.single()
        assertEquals(ServerFailure::class.java.name, attributes["error_type"])
        assertEquals(IOException::class.java.name, attributes["error_cause_type"])
    }

    @Test
    fun `발생 위치를 잃지 않도록 스택트레이스는 보존한다`() {
        val reporter = FakeErrorReporter()
        val original = ServerFailure("문구")

        reporter.recordFailure(original)

        val (recorded, _) = reporter.written.single()
        assertEquals(original.stackTrace.first(), recorded.stackTrace.first())
    }

    @Test
    fun `호출부가 넘긴 속성은 그대로 유지된다`() {
        val reporter = FakeErrorReporter()

        reporter.recordFailure(ServerFailure("문구"), mapOf("auth_stage" to "login"))

        val (_, attributes) = reporter.written.single()
        assertEquals("login", attributes["auth_stage"])
    }
}
