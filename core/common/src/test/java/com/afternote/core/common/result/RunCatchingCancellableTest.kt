package com.afternote.core.common.result

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * [runCatchingCancellable] 회귀 가드 — 취소는 [Result] 로 소비되지 않는다는 규약.
 *
 * 이 규약이 무너지면 취소된 코루틴에서 호출부의 `onFailure` 갈래가 실행되어, 이미 화면을 떠난
 * 사용자에게 스낵바가 뜨거나 죽은 UI 상태가 갱신된다.
 */
class RunCatchingCancellableTest {
    @Test
    fun `블록이 성공하면 그 값을 success 로 감싼다`() =
        runBlocking {
            assertEquals(Result.success("ok"), runCatchingCancellable { "ok" })
        }

    @Test
    fun `일반 예외는 failure 로 감싼다`() =
        runBlocking {
            val failure = runCatchingCancellable { throw IOException("timeout") }

            assertTrue(failure.isFailure)
            assertTrue(failure.exceptionOrNull() is IOException)
        }

    @Test
    fun `CancellationException 은 삼키지 않고 다시 던진다`() =
        runBlocking {
            val thrown =
                try {
                    runCatchingCancellable { throw CancellationException("cancelled") }
                    null
                } catch (e: CancellationException) {
                    e
                }

            assertEquals("cancelled", thrown?.message)
        }

    /**
     * 계약의 실제 목적 — 취소된 코루틴이 취소 상태로 끝나야 한다. stdlib `runCatching` 이었다면
     * 취소가 `Result.failure` 로 소비되어 이 job 이 정상 완료로 끝난다.
     */
    @Test
    fun `취소된 코루틴은 취소 상태로 끝난다`() =
        runBlocking {
            var reachedFailureBranch = false
            val job =
                CoroutineScope(Job()).launch {
                    runCatchingCancellable {
                        throw CancellationException("cancelled")
                    }.onFailure { reachedFailureBranch = true }
                }
            job.join()

            assertTrue(job.isCancelled)
            assertFalse(reachedFailureBranch)
        }
}
