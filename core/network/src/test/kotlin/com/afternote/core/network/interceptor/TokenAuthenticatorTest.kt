package com.afternote.core.network.interceptor

import com.afternote.core.model.TokenBundle
import com.afternote.core.network.FakeAuthRepository
import com.afternote.core.network.token.AccessTokenExpiryTracker
import com.afternote.core.network.token.TokenReissuer
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [TokenAuthenticator] 401 사후 대응 계약 회귀 가드 (#408 에서 코디네이터 경유로 전환).
 * `android.util.Log` 는 `isReturnDefaultValues` 로 no-op — 로그가 끼는 실패 경로도 JVM 에서 돈다.
 */
class TokenAuthenticatorTest {
    private val tracker = AccessTokenExpiryTracker { 0L }

    private fun authenticator(repository: FakeAuthRepository) =
        TokenAuthenticator(
            authRepository = { repository },
            tokenReissuer = TokenReissuer({ repository }, tracker),
        )

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
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onClearSession = { Result.success(Unit) },
            )

        val request = authenticator(repository).authenticate(null, unauthorizedResponse(priorCount = 2))

        assertNull(request)
        assertEquals(0, repository.rotateCallCount)
        assertEquals(1, repository.clearSessionCallCount)
    }

    @Test
    fun `직전 요청에 토큰이 없었음 - 회전·세션 정리 없이 중단`() {
        val repository = FakeAuthRepository(accessToken = "stored")

        val request = authenticator(repository).authenticate(null, unauthorizedResponse(accessToken = null))

        assertNull(request)
        assertEquals(0, repository.rotateCallCount)
        assertEquals(0, repository.clearSessionCallCount)
    }

    @Test
    fun `다른 경로가 이미 회전을 끝냄 - 회전 생략하고 새 토큰으로 재시도`() {
        val repository = FakeAuthRepository(accessToken = "fresh-token")

        val request = authenticator(repository).authenticate(null, unauthorizedResponse(accessToken = "old-token"))

        assertEquals(0, repository.rotateCallCount)
        assertEquals("Bearer fresh-token", request?.header("Authorization"))
    }

    @Test
    fun `회전 성공 - 새 토큰으로 재시도`() {
        val repository =
            FakeAuthRepository(
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
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.success(TokenBundle(accessToken = "old-token", refreshToken = "r")) },
                onClearSession = { Result.success(Unit) },
            )

        val request = authenticator(repository).authenticate(null, unauthorizedResponse())

        assertNull(request)
        assertEquals(1, repository.clearSessionCallCount)
    }

    @Test
    fun `회전 실패 - 세션 정리 후 중단`() {
        val repository =
            FakeAuthRepository(
                accessToken = "old-token",
                onRotateToken = { Result.failure(IllegalStateException("리프레시 만료")) },
                onClearSession = { Result.success(Unit) },
            )

        val request = authenticator(repository).authenticate(null, unauthorizedResponse())

        assertNull(request)
        assertEquals(1, repository.rotateCallCount)
        assertEquals(1, repository.clearSessionCallCount)
    }
}
