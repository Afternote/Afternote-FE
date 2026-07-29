package com.afternote.core.common.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * [ErrorReporter.recordFailure] 의 취소 제외 정책 회귀 가드.
 *
 * 호출부가 넘기는 실패는 대개 `runCatching` 이 만든 `Result` 라 코루틴 취소까지 섞여 들어온다.
 * 화면 이탈이 장애로 기록되면 보관 한도를 잡음이 차지하므로 창구에서 거른다.
 */
class ErrorReporterCancellationTest {
    private class RecordingErrorReporter : ErrorReporter {
        val written = mutableListOf<Pair<Throwable, Map<String, String>>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            written += throwable to attributes
        }
    }

    @Test
    fun `코루틴 취소는 기록하지 않는다`() {
        val reporter = RecordingErrorReporter()

        reporter.recordFailure(CancellationException("화면 이탈"), mapOf("stage" to "home_load"))

        assertTrue(reporter.written.isEmpty())
    }

    @Test
    fun `취소가 아닌 실패는 속성까지 그대로 기록한다`() {
        val reporter = RecordingErrorReporter()
        val failure = IOException("timeout")

        reporter.recordFailure(failure, mapOf("stage" to "home_load"))

        assertEquals(1, reporter.written.size)
        assertEquals(failure, reporter.written.single().first)
        assertEquals(mapOf("stage" to "home_load"), reporter.written.single().second)
    }
}
