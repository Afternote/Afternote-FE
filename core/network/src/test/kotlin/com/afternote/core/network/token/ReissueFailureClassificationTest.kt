package com.afternote.core.network.token

import com.afternote.core.network.FakeAuthRepository
import com.afternote.core.network.FakeErrorReporter
import com.afternote.core.network.FakeInterceptorChain
import com.afternote.core.network.interceptor.ApiErrorInterceptor
import com.afternote.core.network.jsonResponse
import com.afternote.core.network.model.ApiException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "서버가 이렇게 응답하면 재발급은 이렇게 분류된다" 를 **응답 본문에서 분류까지** 잠그는 가드 (#1126).
 *
 * [TokenReissuerTest] 는 `rotateToken()` 이 던지는 예외를 가짜가 직접 주입하므로, 정작 그 예외를
 * 만드는 구간 — `ApiErrorInterceptor` 의 본문 파싱 — 이 테스트에서 빠져 있었다. 이 이슈의 위험이
 * 정확히 그 구간에 있다: 재발급 400 의 `code=1107` 은 **파싱에 성공해야** 읽히고, 실패하면
 * `code` 자리에 HTTP 상태(400)가 들어간다([ApiErrorInterceptor] 참조). 그래서 여기서는
 * 실제 인터셉터로 예외를 만들어 [TokenReissuer] 에 먹인다.
 *
 * 재발급은 토큰 미부착 `RefreshClient` 를 타는데 거기에도 `ApiErrorInterceptor` 가 붙어 있어
 * (`NetworkModule.provideRefreshOkHttpClient`), 재발급 실패는 항상 [ApiException] 으로 온다.
 */
class ReissueFailureClassificationTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val tracker = AccessTokenExpiryTracker { 0L }

    /** 실제 [ApiErrorInterceptor] 에 이 응답을 먹여 나온 예외 — 프로덕션이 만드는 것과 같은 값이다. */
    private fun failureFrom(
        status: Int,
        body: String,
        contentType: String = "application/json",
    ): Throwable =
        runCatching {
            ApiErrorInterceptor(json).intercept(
                FakeInterceptorChain(respond = { it.jsonResponse(code = status, body = body, contentType = contentType) }),
            )
        }.exceptionOrNull() ?: error("ApiErrorInterceptor 가 $status 를 예외로 바꾸지 않았다")

    private fun outcomeOf(failure: Throwable): TokenReissuer.Outcome {
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
                onClearSession = {
                    accessToken = null
                    Result.success(Unit)
                },
            )
        return TokenReissuer({ repository }, tracker, FakeErrorReporter())
            .reissue(expectedAccessToken = "old-token")
    }

    @Test
    fun `재발급 400 은 본문이 무엇이든 인증 거절이다`() {
        val bodies =
            mapOf(
                "code 를 담은 정상 봉투" to """{"status":400,"code":1107,"message":"유효하지 않은 리프레시 토큰","data":null}""",
                "빈 본문" to "",
                "게이트웨이 HTML" to "<html><head><title>400 Bad Request</title></head><body>400</body></html>",
                "잘린 JSON" to """{"status":400,"code":11""",
                "code 없는 봉투" to """{"message":"bad request"}""",
            )
        bodies.forEach { (label, body) ->
            val contentType = if (label == "게이트웨이 HTML") "text/html" else "application/json"
            val failure = failureFrom(status = 400, body = body, contentType = contentType)

            // 파싱이 실패하면 code 자리에 HTTP 상태가 들어간다 — 1107 로 거를 수 없는 이유다.
            assertTrue(label, failure is ApiException)
            val outcome = outcomeOf(failure)

            assertTrue(
                "$label -> ${outcome::class.simpleName}",
                outcome is TokenReissuer.Outcome.AuthenticationRejected,
            )
        }
    }

    @Test
    fun `파싱 실패한 400 의 code 자리에는 HTTP 상태가 들어온다`() {
        // 이 값이 1107 이 아니라는 것이 «본문 파싱에 기대면 안 된다» 의 근거다.
        val failure = failureFrom(status = 400, body = "") as ApiException

        assertEquals(400, failure.status)
        assertEquals(400, failure.code)
        assertEquals(null, failure.serverMessage)
    }

    @Test
    fun `재발급 5xx 는 여전히 서버 실패다 - 400 만 좁혔다`() {
        val outcome = outcomeOf(failureFrom(status = 503, body = ""))

        assertTrue(outcome is TokenReissuer.Outcome.ServerFailure)
    }

    @Test
    fun `재발급 404 는 여전히 미분류다 - 400 만 좁혔다`() {
        val outcome = outcomeOf(failureFrom(status = 404, body = ""))

        assertTrue(outcome is TokenReissuer.Outcome.UnexpectedFailure)
    }
}
