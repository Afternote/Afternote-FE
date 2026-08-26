package com.afternote.core.network.token

import com.afternote.core.model.TokenBundle
import com.afternote.core.network.FakeAuthRepository
import com.afternote.core.network.FakeErrorReporter
import com.afternote.core.network.model.ApiException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import java.net.SocketTimeoutException
import retrofit2.Response as RetrofitResponse

/**
 * [TokenReissuer] 단일 비행 계약 회귀 가드 (#408).
 * 핵심 계약 — "관찰 토큰 vs 저장 토큰" 재확인으로 늦게 진입한 경로는 회전을 생략하고,
 * 회전 성공 시 발급 응답(#410)의 `expiresIn` 으로 deadline 을 기록(미동봉이면 폐기),
 * 회전 실패 시 deadline 을 폐기한다.
 *
 * 실패 경로 계약 (#1126) — 재발급 400 은 본문과 무관하게 인증 거절이고, 확정 거절은 락 안에서
 * 세션 정리까지 끝낸 뒤 그 액세스 토큰에 묶여 공유된다(일시 실패는 공유하지 않는다).
 */
class TokenReissuerTest {
    private var nowElapsedMillis = 0L
    private val tracker = AccessTokenExpiryTracker { nowElapsedMillis }

    private fun reissuer(
        repository: FakeAuthRepository,
        reporter: FakeErrorReporter = FakeErrorReporter(),
    ) = TokenReissuer({ repository }, tracker, reporter)

    private fun httpFailure(status: Int): HttpException = HttpException(RetrofitResponse.error<Unit>(status, "".toResponseBody()))

    @Test
    fun `저장 토큰이 관찰 토큰과 다름 - 회전 생략하고 갱신된 토큰 반환`() {
        val repository = FakeAuthRepository(accessToken = "refreshed-by-other-path")
        val coordinator = reissuer(repository)

        val outcome = coordinator.reissue(expectedAccessToken = "old-token")

        assertEquals(0, repository.rotateCallCount)
        assertEquals(
            TokenReissuer.Outcome.TokenAlreadyChanged("refreshed-by-other-path"),
            outcome,
        )
    }

    @Test
    fun `회전 성공 - 발급 응답 expiresIn 으로 deadline 기록`() {
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = {
                    accessToken = "fresh-token"
                    Result.success(
                        TokenBundle(accessToken = "fresh-token", refreshToken = "r", expiresIn = 3599),
                    )
                },
            )
        val coordinator = reissuer(repository)

        val outcome = coordinator.reissue(expectedAccessToken = "old-token")

        assertEquals(1, repository.rotateCallCount)
        assertEquals(TokenReissuer.Outcome.Rotated("fresh-token"), outcome)
        // 새 토큰 수명 3599초 기록 → 3540초 경과 후 잔여 59초 < 임계 60초
        nowElapsedMillis += 3_540_000L
        assertTrue(tracker.isExpiringSoon())
    }

    @Test
    fun `회전 성공했지만 expiresIn 미동봉 - deadline 폐기`() {
        tracker.record(expiresInSeconds = 30)
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = {
                    accessToken = "fresh-token"
                    Result.success(TokenBundle(accessToken = "fresh-token", refreshToken = "r"))
                },
            )
        val coordinator = reissuer(repository)

        val outcome = coordinator.reissue(expectedAccessToken = "old-token")

        assertEquals(TokenReissuer.Outcome.Rotated("fresh-token"), outcome)
        // expiresIn 이 없으면 직전 deadline 을 비워 선제 갱신을 쉰다 (401 안전망에 위임)
        nowElapsedMillis += Long.MAX_VALUE / 2
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `refresh 인증 거절 401 403 - 원인 보존하고 리포팅 제외`() {
        listOf(401, 403).forEach { status ->
            val failure = httpFailure(status)
            val reporter = FakeErrorReporter()
            val repository =
                FakeAuthRepository(
                    accessToken = "old-token",
                    onRotateToken = { Result.failure(failure) },
                    onClearSession = { Result.success(Unit) },
                )

            val outcome = reissuer(repository, reporter).reissue(expectedAccessToken = "old-token")

            assertTrue(outcome is TokenReissuer.Outcome.AuthenticationRejected)
            assertSame(failure, (outcome as TokenReissuer.Outcome.AuthenticationRejected).exception)
            assertTrue(reporter.writtenFailures.isEmpty())
            assertEquals(1, repository.clearSessionCallCount)
        }
    }

    @Test
    fun `refresh 무효 400 code 1107 - 인증 거절로 분류하고 리포팅 제외`() {
        val failure =
            ApiException(
                status = 400,
                code = 1107,
                serverMessage = "유효하지 않은 리프레시 토큰",
                message = "유효하지 않은 리프레시 토큰",
            )
        val reporter = FakeErrorReporter()
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
                onClearSession = { Result.success(Unit) },
            )

        val outcome = reissuer(repository, reporter).reissue(expectedAccessToken = "old-token")

        assertTrue(outcome is TokenReissuer.Outcome.AuthenticationRejected)
        assertSame(failure, (outcome as TokenReissuer.Outcome.AuthenticationRejected).exception)
        assertTrue(reporter.writtenFailures.isEmpty())
        assertEquals(1, repository.clearSessionCallCount)
    }

    @Test
    fun `transport 실패 - 원인 보존, deadline 폐기, 비민감 non-fatal 기록`() {
        tracker.record(expiresInSeconds = 30)
        val secret = "refresh-token-secret"
        val failure = SocketTimeoutException("timeout while sending $secret")
        val reporter = FakeErrorReporter()
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
            )
        val coordinator = reissuer(repository, reporter)

        val outcome = coordinator.reissue(expectedAccessToken = "old-token")

        assertTrue(outcome is TokenReissuer.Outcome.TransportFailure)
        assertSame(failure, (outcome as TokenReissuer.Outcome.TransportFailure).exception)
        assertFalse(tracker.isExpiringSoon())
        assertEquals(0, repository.clearSessionCallCount)
        val (reported, attributes) = reporter.writtenFailures.single()
        assertEquals("token_reissue", attributes["auth_stage"])
        assertEquals("transport", attributes["failure_kind"])
        assertEquals(SocketTimeoutException::class.java.name, attributes["error_type"])
        assertEquals(SocketTimeoutException::class.java.name, reported.message)
        assertTrue(secret !in reported.toString())
        assertTrue(secret !in attributes.toString())
    }

    @Test
    fun `5xx 실패 - 원인 보존하고 non-fatal 1건 기록`() {
        val failure =
            ApiException(
                status = 503,
                code = 503,
                serverMessage = null,
                message = "temporary server failure",
            )
        val reporter = FakeErrorReporter()
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
            )

        val outcome = reissuer(repository, reporter).reissue(expectedAccessToken = "old-token")

        assertTrue(outcome is TokenReissuer.Outcome.ServerFailure)
        assertSame(failure, (outcome as TokenReissuer.Outcome.ServerFailure).exception)
        val (_, attributes) = reporter.writtenFailures.single()
        assertEquals("token_reissue", attributes["auth_stage"])
        assertEquals("server", attributes["failure_kind"])
        assertEquals(ApiException::class.java.name, attributes["error_type"])
    }

    @Test
    fun `회전 성공했지만 액세스 토큰이 빈 값 - UnexpectedFailure 반환`() {
        tracker.record(expiresInSeconds = 30)
        val reporter = FakeErrorReporter()
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = {
                    Result.success(TokenBundle(accessToken = "", refreshToken = "refresh-token"))
                },
            )

        val outcome = reissuer(repository, reporter).reissue(expectedAccessToken = "old-token")

        assertTrue(outcome is TokenReissuer.Outcome.UnexpectedFailure)
        assertFalse(tracker.isExpiringSoon())
        val (_, attributes) = reporter.writtenFailures.single()
        assertEquals("token_reissue", attributes["auth_stage"])
        assertEquals("unexpected", attributes["failure_kind"])
        assertEquals(0, repository.clearSessionCallCount)
    }

    @Test
    fun `code 파싱 실패한 400 - 본문과 무관하게 인증 거절로 확정`() {
        // 무효 refresh 의 code=1107 은 본문 파싱에 성공해야 읽힌다. 파싱이 안 되면 400 은
        // 401·403·5xx 어디에도 안 걸려 "세션 유지" 로 떨어졌고, 그 길로 무효 refresh 가 남았다 (#1126).
        listOf(
            httpFailure(400),
            ApiException(status = 400, code = 1999, serverMessage = "unparsed", message = "unparsed"),
        ).forEach { failure ->
            val reporter = FakeErrorReporter()
            val repository =
                FakeAuthRepository(
                    accessToken = "old-token",
                    onRotateToken = { Result.failure(failure) },
                    onClearSession = { Result.success(Unit) },
                )

            val outcome = reissuer(repository, reporter).reissue(expectedAccessToken = "old-token")

            assertTrue(outcome is TokenReissuer.Outcome.AuthenticationRejected)
            assertSame(failure, (outcome as TokenReissuer.Outcome.AuthenticationRejected).exception)
            assertEquals(1, repository.clearSessionCallCount)
            assertTrue(reporter.writtenFailures.isEmpty())
        }
    }

    @Test
    fun `미분류 4xx 실패 - UnexpectedFailure 로 분류하고 non-fatal 기록`() {
        val failure =
            ApiException(
                status = 404,
                code = 1999,
                serverMessage = "new server error",
                message = "new server error",
            )
        val reporter = FakeErrorReporter()
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
            )

        val outcome = reissuer(repository, reporter).reissue(expectedAccessToken = "old-token")

        assertTrue(outcome is TokenReissuer.Outcome.UnexpectedFailure)
        assertSame(failure, (outcome as TokenReissuer.Outcome.UnexpectedFailure).exception)
        val (reported, attributes) = reporter.writtenFailures.single()
        assertEquals("unexpected", attributes["failure_kind"])
        assertEquals(ApiException::class.java.name, reported.message)
        assertTrue("new server error" !in reported.toString())
        assertTrue("new server error" !in attributes.toString())
    }

    @Test
    fun `저장 토큰이 빈 값 - TokenAlreadyChanged 가 아니라 회전 시도로 진행`() {
        val repository =
            FakeAuthRepository(
                accessToken = null,
                onRotateToken = { Result.failure(IllegalStateException("리프레시 토큰이 존재하지 않습니다.")) },
            )
        val coordinator = reissuer(repository)

        val outcome = coordinator.reissue(expectedAccessToken = "old-token")

        assertEquals(1, repository.rotateCallCount)
        assertTrue(outcome is TokenReissuer.Outcome.UnexpectedFailure)
    }

    @Test
    fun `확정 거절 뒤 같은 토큰으로 재진입 - 재발급 없이 같은 결과를 공유`() {
        val failure = httpFailure(400)
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(failure) },
                onClearSession = {
                    // 실제 clearSession 과 같이 저장 토큰이 사라진 상태를 재현한다.
                    accessToken = null
                    Result.success(Unit)
                },
            )
        val coordinator = reissuer(repository)

        val first = coordinator.reissue(expectedAccessToken = "old-token")
        val second = coordinator.reissue(expectedAccessToken = "old-token")
        val third = coordinator.reissue(expectedAccessToken = "old-token")

        // 재발급 HTTP 와 세션 정리는 승자 1회뿐, 나머지는 같은 분류를 그대로 받는다.
        assertEquals(1, repository.rotateCallCount)
        assertEquals(1, repository.clearSessionCallCount)
        assertSame(first, second)
        assertSame(first, third)
    }

    @Test
    fun `일시 실패는 공유하지 않는다 - 다음 호출이 다시 재발급을 시도`() {
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(SocketTimeoutException("timeout")) },
            )
        val coordinator = reissuer(repository)

        coordinator.reissue(expectedAccessToken = "old-token")
        val outcome = coordinator.reissue(expectedAccessToken = "old-token")

        // 재시도가 성립하는 실패다 — 캐시하면 네트워크 복구 뒤에도 영영 못 살아난다.
        assertEquals(2, repository.rotateCallCount)
        assertTrue(outcome is TokenReissuer.Outcome.TransportFailure)
        assertEquals(0, repository.clearSessionCallCount)
    }

    @Test
    fun `거절 뒤 재로그인 - 새 토큰은 캐시에 걸리지 않는다`() {
        var rotateFails = true
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = {
                    if (rotateFails) {
                        Result.failure(httpFailure(400))
                    } else {
                        accessToken = "rotated-token"
                        Result.success(TokenBundle(accessToken = "rotated-token", refreshToken = "r"))
                    }
                },
                onClearSession = {
                    accessToken = null
                    Result.success(Unit)
                },
            )
        val coordinator = reissuer(repository)

        coordinator.reissue(expectedAccessToken = "old-token")
        // 재로그인이 준 새 토큰 — 거절 캐시의 키(옛 액세스 토큰)와 어긋나므로 정상 회전으로 간다.
        rotateFails = false
        repository.accessToken = "relogin-token"
        val outcome = coordinator.reissue(expectedAccessToken = "relogin-token")

        assertEquals(TokenReissuer.Outcome.Rotated("rotated-token"), outcome)
        assertEquals(2, repository.rotateCallCount)
    }
}
