package com.afternote.core.network.interceptor

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.FakeErrorReporter
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.networkFakeAuthRepository
import com.afternote.core.network.token.AccessTokenExpiryTracker
import com.afternote.core.network.token.TokenReissuer
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import retrofit2.Response as RetrofitResponse

/**
 * [TokenAuthenticator] 401 사후 대응 계약 회귀 가드 (#408 에서 코디네이터 경유로 전환).
 * 자체 계약 위반은 [FakeErrorReporter] 로 보고하고, [TokenReissuer] 가 이미 분류한 결과는 중복 보고하지 않는다.
 *
 * 재발급 실패 시 던지는 예외의 **타입 이름** 은 계약이 아니다 — OkHttp `Authenticator` 가
 * 요구하는 것은 `IOException` 이고, 화면이 보는 것은 «기술 원문 없이(message == null) 원인만
 * 매달린 채 이번 요청만 실패» 다. 그래서 구현 클래스는 파일 밖으로 열지 않고 그 계약만 단언한다 (#1672).
 */
class TokenAuthenticatorTest {
    private val tracker = AccessTokenExpiryTracker { 0L }

    private fun authenticator(
        repository: FakeAuthRepository,
        reporter: FakeErrorReporter = FakeErrorReporter(),
    ) = TokenAuthenticator(
        authRepository = { repository },
        tokenReissuer = TokenReissuer({ repository }, tracker, reporter),
        errorReporter = reporter,
    )

    private fun httpFailure(status: Int): HttpException = HttpException(RetrofitResponse.error<Unit>(status, "".toResponseBody()))

    private fun unauthorizedResponse(
        accessToken: String? = "old-token",
        priorCount: Int = 0,
    ): Response {
        val requestBuilder = Request.Builder().url("https://afternote.kro.kr/api/v1/test")
        if (accessToken != null) requestBuilder.header("Authorization", "Bearer $accessToken")
        val request = requestBuilder.build()

        var prior: Response? = null
        repeat(priorCount) {
            prior =
                Response
                    .Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .priorResponse(prior)
                    .build()
        }
        return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .priorResponse(prior)
            .build()
    }

    @Test
    fun `재시도 3회 도달 - 회전 없이 세션 정리 후 중단`() {
        val reporter = FakeErrorReporter()
        val repository =
            networkFakeAuthRepository(
                accessToken = "old-token",
                onClearSession = { Result.success(Unit) },
            )

        val request = authenticator(repository, reporter).authenticate(null, unauthorizedResponse(priorCount = 2))

        assertNull(request)
        assertEquals(0, repository.rotateCallCount)
        assertEquals(1, repository.clearSessionCallCount)
        assertContractViolation(reporter, "retry_limit")
    }

    @Test
    fun `직전 요청에 토큰이 없었음 - 회전·세션 정리 없이 중단`() {
        val reporter = FakeErrorReporter()
        val repository = networkFakeAuthRepository(accessToken = "stored")

        val request = authenticator(repository, reporter).authenticate(null, unauthorizedResponse(accessToken = null))

        assertNull(request)
        assertEquals(0, repository.rotateCallCount)
        assertEquals(0, repository.clearSessionCallCount)
        assertContractViolation(reporter, "missing_auth_header")
    }

    @Test
    fun `다른 경로가 이미 회전을 끝냄 - 회전 생략하고 새 토큰으로 재시도`() {
        val repository = networkFakeAuthRepository(accessToken = "fresh-token")

        val request = authenticator(repository).authenticate(null, unauthorizedResponse(accessToken = "old-token"))

        assertEquals(0, repository.rotateCallCount)
        assertEquals("Bearer fresh-token", request?.header("Authorization"))
    }

    @Test
    fun `회전 성공 - 새 토큰으로 재시도`() {
        val repository =
            networkFakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = {
                    accessToken = "fresh-token"
                    Result.success(TokenBundle(accessToken = "fresh-token", refreshToken = "r"))
                },
            )

        val request = authenticator(repository).authenticate(null, unauthorizedResponse())

        assertEquals(1, repository.rotateCallCount)
        assertEquals("Bearer fresh-token", request?.header("Authorization"))
        assertEquals(0, repository.clearSessionCallCount)
    }

    @Test
    fun `서버가 동일 토큰을 반환 - 세션 정리 후 중단 (무한 재시도 방지)`() {
        val reporter = FakeErrorReporter()
        val repository =
            networkFakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.success(TokenBundle(accessToken = "old-token", refreshToken = "r")) },
                onClearSession = { Result.success(Unit) },
            )

        val request = authenticator(repository, reporter).authenticate(null, unauthorizedResponse())

        assertNull(request)
        assertEquals(1, repository.clearSessionCallCount)
        assertContractViolation(reporter, "same_token")
    }

    @Test
    fun `refresh 인증 거절 401 403 - 세션 정리 후 중단`() {
        listOf(401, 403).forEach { status ->
            val reporter = FakeErrorReporter()
            val repository =
                networkFakeAuthRepository(
                    accessToken = "old-token",
                    onRotateToken = { Result.failure(httpFailure(status)) },
                    onClearSession = { Result.success(Unit) },
                )

            val request = authenticator(repository, reporter).authenticate(null, unauthorizedResponse())

            assertNull(request)
            assertEquals(1, repository.rotateCallCount)
            assertEquals(1, repository.clearSessionCallCount)
            assertEquals(0, reporter.writtenFailures.size)
        }
    }

    @Test
    fun `refresh 무효 400 code 1107 - 세션 정리 후 중단`() {
        val reporter = FakeErrorReporter()
        val failure =
            ApiException(
                status = 400,
                code = 1107,
                serverMessage = "유효하지 않은 리프레시 토큰",
                fallbackMessage = "유효하지 않은 리프레시 토큰",
            )
        val repository =
            networkFakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
                onClearSession = { Result.success(Unit) },
            )

        val request = authenticator(repository, reporter).authenticate(null, unauthorizedResponse())

        assertNull(request)
        assertEquals(1, repository.rotateCallCount)
        assertEquals(1, repository.clearSessionCallCount)
        assertEquals(0, reporter.writtenFailures.size)
    }

    @Test
    fun `code 파싱 실패한 400 - 세션 정리 후 중단`() {
        val reporter = FakeErrorReporter()
        // 본문 파싱이 안 되면 400 이 "세션 유지" 로 떨어져 무효 refresh 가 남았다 (#1126).
        val repository =
            networkFakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(httpFailure(400)) },
                onClearSession = { Result.success(Unit) },
            )

        val request = authenticator(repository, reporter).authenticate(null, unauthorizedResponse())

        assertNull(request)
        assertEquals(1, repository.rotateCallCount)
        assertEquals(1, repository.clearSessionCallCount)
        assertEquals(0, reporter.writtenFailures.size)
    }

    @Test
    fun `refresh timeout - 세션 유지하고 현재 요청만 실패`() {
        val reporter = FakeErrorReporter()
        val failure = SocketTimeoutException("temporary timeout")
        val repository =
            networkFakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
            )

        val thrown =
            assertThrows(IOException::class.java) {
                authenticator(repository, reporter).authenticate(null, unauthorizedResponse())
            }

        assertNull(thrown.message)
        assertSame(failure, thrown.cause)
        assertEquals(1, repository.rotateCallCount)
        assertEquals(0, repository.clearSessionCallCount)
        assertReissueFailure(reporter, failureKind = "transport", errorType = SocketTimeoutException::class.java.name)
    }

    @Test
    fun `refresh 5xx - 세션 유지하고 현재 요청만 실패`() {
        val reporter = FakeErrorReporter()
        val failure = httpFailure(503)
        val repository =
            networkFakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
            )

        val thrown =
            assertThrows(IOException::class.java) {
                authenticator(repository, reporter).authenticate(null, unauthorizedResponse())
            }

        assertNull(thrown.message)
        assertSame(failure, thrown.cause)
        assertEquals(1, repository.rotateCallCount)
        assertEquals(0, repository.clearSessionCallCount)
        assertReissueFailure(reporter, failureKind = "server", errorType = HttpException::class.java.name)
    }

    @Test
    fun `동시 401 - 재발급은 한 번만 나가고 분류가 갈리지 않는다`() {
        // 실기에서 이 경합은 확률적이라 잡히지 않는다(창이 3ms). 여기서는 세션 정리를 느리게 만들어
        // 창을 열어 둔 채 결정적으로 재현한다 — 실제 clearSession 도 DataStore I/O 다 (#1126).
        val repository =
            networkFakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(httpFailure(400)) },
                onClearSession = {
                    Thread.sleep(SLOW_SESSION_CLEAR_MILLIS)
                    accessToken = null
                    Result.success(Unit)
                },
            )
        val authenticator = authenticator(repository)
        val results = ConcurrentLinkedQueue<String>()
        val startTogether = CountDownLatch(1)
        val threads =
            List(CONCURRENT_CALLERS) {
                Thread {
                    startTogether.await()
                    results +=
                        runCatching { authenticator.authenticate(null, unauthorizedResponse()) }
                            .fold(
                                onSuccess = { if (it == null) "요청 중단" else "재시도" },
                                onFailure = { "예외 ${it.javaClass.simpleName}" },
                            )
                }
            }
        threads.forEach { it.start() }
        startTogether.countDown()
        threads.forEach { it.join(THREAD_JOIN_TIMEOUT_MILLIS) }

        // 정리가 락 밖에 있으면 대기자가 옛 저장 토큰을 보고 각자 재발급을 친다 — 그 창을 막았는지 본다.
        assertEquals(1, repository.rotateCallCount)
        assertEquals(1, repository.clearSessionCallCount)
        // 같은 실패에 「세션 만료」와 「세션 유지」가 함께 남던 것이 이 이슈의 증상이다.
        assertEquals(listOf("요청 중단"), results.distinct())
    }

    private fun assertContractViolation(
        reporter: FakeErrorReporter,
        authStage: String,
    ) {
        val (_, attributes) = reporter.writtenFailures.single()
        assertEquals(
            mapOf(
                "auth_stage" to authStage,
                "error_type" to IllegalStateException::class.java.name,
            ),
            attributes,
        )
    }

    private fun assertReissueFailure(
        reporter: FakeErrorReporter,
        failureKind: String,
        errorType: String,
    ) {
        val (_, attributes) = reporter.writtenFailures.single()
        assertEquals(
            mapOf(
                "auth_stage" to "token_reissue",
                "failure_kind" to failureKind,
                "error_type" to errorType,
            ),
            attributes,
        )
    }
}

private const val CONCURRENT_CALLERS = 5
private const val SLOW_SESSION_CLEAR_MILLIS = 80L
private const val THREAD_JOIN_TIMEOUT_MILLIS = 5_000L
