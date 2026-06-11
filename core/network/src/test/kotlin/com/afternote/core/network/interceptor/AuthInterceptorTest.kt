package com.afternote.core.network.interceptor

import com.afternote.core.model.TokenBundle
import com.afternote.core.network.FakeAuthRepository
import com.afternote.core.network.token.AccessTokenExpiryTracker
import com.afternote.core.network.token.TokenReissuer
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * [AuthInterceptor] 선제 reissue·`expiresIn` 수신 동작 회귀 가드 (#408).
 *
 * 가짜 시계·가짜 chain 기반 — 검증 계약:
 * 1. 기록된 deadline 이 없으면 reissue 없이 기존 토큰 부착
 * 2. 만료 임박이면 요청 전 reissue 후 새 토큰 부착
 * 3. 선제 reissue 실패는 best-effort — 기존 토큰으로 진행하고 clearSession 하지 않음
 *    (Fake 의 clearSession 이 error 를 던지므로 호출 자체가 테스트 실패로 드러난다)
 * 4. 성공 응답 봉투의 expiresIn 을 deadline 으로 기록
 * 5. 기록 가드 — 실패 응답·비JSON·비봉투 본문·JSON null·rotate 가 끼어든 stale 응답은 무기록
 * 6. 토큰 부재 시 미부착 + stale deadline 폐기
 */
class AuthInterceptorTest {
    private var nowElapsedMillis = 0L
    private val tracker = AccessTokenExpiryTracker { nowElapsedMillis }
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    private fun interceptor(repository: FakeAuthRepository) =
        AuthInterceptor(
            authRepository = { repository },
            expiryTracker = tracker,
            tokenReissuer = TokenReissuer({ repository }, tracker),
            json = json,
        )

    @Test
    fun `기록된 deadline 없음 - reissue 없이 저장된 토큰 부착`() {
        val repository = FakeAuthRepository(accessToken = "stored-token")
        val chain = RecordingChain()

        interceptor(repository).intercept(chain)

        assertEquals(0, repository.rotateCallCount)
        assertEquals("Bearer stored-token", chain.sentRequest?.header("Authorization"))
    }

    @Test
    fun `만료 임박 - 요청 전 선제 reissue 후 새 토큰 부착`() {
        tracker.record(expiresInSeconds = 30)
        val repository =
            FakeAuthRepository(
                accessToken = "stale-token",
                onRotateToken = {
                    accessToken = "fresh-token"
                    Result.success(TokenBundle(accessToken = "fresh-token", refreshToken = "r"))
                },
            )
        val chain = RecordingChain()

        interceptor(repository).intercept(chain)

        assertEquals(1, repository.rotateCallCount)
        assertEquals("Bearer fresh-token", chain.sentRequest?.header("Authorization"))
        // 새 토큰 수명은 다음 목록 응답에서 다시 기록됨 — 직전 deadline 은 폐기됐어야 한다
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `선제 reissue 실패 - 기존 토큰으로 진행하고 deadline 폐기`() {
        tracker.record(expiresInSeconds = 30)
        val repository =
            FakeAuthRepository(
                accessToken = "stored-token",
                onRotateToken = { Result.failure(IllegalStateException("일시적 네트워크 오류")) },
            )
        val chain = RecordingChain()

        interceptor(repository).intercept(chain)

        assertEquals(1, repository.rotateCallCount)
        assertEquals("Bearer stored-token", chain.sentRequest?.header("Authorization"))
        // 만료 deadline 잔존 시 매 요청 reissue 폭주 — clear 됐어야 한다
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `성공 응답 봉투의 expiresIn - deadline 으로 기록`() {
        val repository = FakeAuthRepository(accessToken = "stored-token")
        val chain =
            RecordingChain(
                responseBodyJson =
                    """{"status":200,"code":200,"message":"성공","data":null,"expiresIn":3599}""",
            )

        interceptor(repository).intercept(chain)

        // 3599초 중 3540초 경과 → 잔여 59초 < 임계 60초
        nowElapsedMillis += 3_540_000L
        assertTrue(tracker.isExpiringSoon())
    }

    @Test
    fun `expiresIn 없는 응답 - deadline 기록 없음 유지`() {
        val repository = FakeAuthRepository(accessToken = "stored-token")
        val chain =
            RecordingChain(
                responseBodyJson = """{"status":200,"code":200,"message":"성공","data":null}""",
            )

        interceptor(repository).intercept(chain)

        nowElapsedMillis += Long.MAX_VALUE / 2
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `실패 응답 봉투에 expiresIn 이 있어도 - 무기록`() {
        val repository = FakeAuthRepository(accessToken = "stored-token")
        val chain =
            RecordingChain(
                responseCode = 400,
                responseBodyJson =
                    """{"status":400,"code":1902,"message":"실패","data":null,"expiresIn":3599}""",
            )

        interceptor(repository).intercept(chain)

        assertNeverRecorded()
    }

    @Test
    fun `JSON 이 아닌 응답 본문 - peek 없이 무기록`() {
        val repository = FakeAuthRepository(accessToken = "stored-token")
        val chain =
            RecordingChain(
                responseBodyJson = """본문에 "expiresIn" 리터럴이 있어도 게이트에서 걸러진다""",
                responseContentType = "text/plain",
            )

        interceptor(repository).intercept(chain)

        assertNeverRecorded()
    }

    @Test
    fun `봉투 형태가 아닌 본문에 expiresIn 리터럴 포함 - 디코드 실패로 무기록`() {
        val repository = FakeAuthRepository(accessToken = "stored-token")
        val chain = RecordingChain(responseBodyJson = """[{"expiresIn":3599}]""")

        interceptor(repository).intercept(chain)

        assertNeverRecorded()
    }

    @Test
    fun `expiresIn 이 JSON null - 무기록`() {
        val repository = FakeAuthRepository(accessToken = "stored-token")
        val chain =
            RecordingChain(
                responseBodyJson = """{"status":200,"code":200,"data":null,"expiresIn":null}""",
            )

        interceptor(repository).intercept(chain)

        assertNeverRecorded()
    }

    @Test
    fun `응답 in-flight 사이 토큰이 회전됨 - 구 토큰 기준 expiresIn 폐기`() {
        val repository = FakeAuthRepository(accessToken = "old-token")
        val chain =
            RecordingChain(
                responseBodyJson =
                    """{"status":200,"code":200,"data":null,"expiresIn":40}""",
                // 요청이 나간 뒤(=proceed 중) 다른 경로가 회전을 끝낸 상황 재현
                onProceed = { repository.accessToken = "rotated-token" },
            )

        interceptor(repository).intercept(chain)

        // 구 토큰 기준 잔여 40초가 새 토큰 deadline 으로 기록되면 불필요 회전이 유발된다
        assertNeverRecorded()
    }

    @Test
    fun `토큰 부재 - 헤더 미부착 및 stale deadline 폐기`() {
        tracker.record(expiresInSeconds = 30)
        val repository = FakeAuthRepository(accessToken = null)
        val chain = RecordingChain()

        interceptor(repository).intercept(chain)

        assertEquals(0, repository.rotateCallCount)
        assertNull(chain.sentRequest?.header("Authorization"))
        assertFalse(tracker.isExpiringSoon())
    }

    /** deadline 이 전혀 기록되지 않았음을 단언 — 아무리 시간이 흘러도 임박 판정이 나오면 안 된다. */
    private fun assertNeverRecorded() {
        nowElapsedMillis += Long.MAX_VALUE / 2
        assertFalse(tracker.isExpiringSoon())
    }
}

/** [proceed] 에 들어온 요청을 캡처하고 준비된 JSON 봉투로 응답을 돌려주는 가짜 chain. */
private class RecordingChain(
    private val responseBodyJson: String = """{"status":200,"code":200,"message":"성공","data":null}""",
    private val responseCode: Int = 200,
    private val responseContentType: String = "application/json",
    private val onProceed: () -> Unit = {},
) : Interceptor.Chain {
    var sentRequest: Request? = null
        private set

    override fun request(): Request = Request.Builder().url("https://afternote.kro.kr/api/v1/test").build()

    override fun proceed(request: Request): Response {
        sentRequest = request
        onProceed()
        return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(responseCode)
            .message(if (responseCode == 200) "OK" else "Error")
            .body(responseBodyJson.toResponseBody(responseContentType.toMediaType()))
            .build()
    }

    override fun connection(): Connection? = null

    override fun call(): Call = error("not used")

    override fun connectTimeoutMillis(): Int = 0

    override fun withConnectTimeout(
        timeout: Int,
        unit: TimeUnit,
    ): Interceptor.Chain = this

    override fun readTimeoutMillis(): Int = 0

    override fun withReadTimeout(
        timeout: Int,
        unit: TimeUnit,
    ): Interceptor.Chain = this

    override fun writeTimeoutMillis(): Int = 0

    override fun withWriteTimeout(
        timeout: Int,
        unit: TimeUnit,
    ): Interceptor.Chain = this
}
