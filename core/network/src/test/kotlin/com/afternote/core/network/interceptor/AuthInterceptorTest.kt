package com.afternote.core.network.interceptor

import com.afternote.core.model.TokenBundle
import com.afternote.core.network.FakeAuthRepository
import com.afternote.core.network.FakeErrorReporter
import com.afternote.core.network.token.AccessTokenExpiryTracker
import com.afternote.core.network.token.TokenReissuer
import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.Call
import okhttp3.CertificatePinner
import okhttp3.Connection
import okhttp3.ConnectionPool
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import java.net.Proxy
import java.net.ProxySelector
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import retrofit2.Response as RetrofitResponse

/**
 * [AuthInterceptor] 선제 reissue 동작 회귀 가드 (#408).
 *
 * 가짜 시계·가짜 chain 기반 — 검증 계약:
 * 1. 기록된 deadline 이 없으면 reissue 없이 기존 토큰 부착
 * 2. 만료 임박이면 요청 전 reissue 후 새 토큰 부착
 * 3. 선제 reissue 의 **일시** 실패는 best-effort — 기존 토큰으로 진행하고 clearSession 하지 않음
 *    (Fake 의 clearSession 이 error 를 던지므로 호출 자체가 테스트 실패로 드러난다).
 *    refresh 가 거절당한 확정 실패는 예외다 — 락 안에서 세션 정리까지 끝난다 (#1126)
 * 4. 토큰 부재 시 미부착 + stale deadline 폐기
 *
 * `expiresIn` 수신은 발급 응답(로그인·reissue)에서 처리되므로(#410) 더 이상 인터셉터가 응답을
 * peek 하지 않는다 — 수신 계약은 [TokenReissuerTest]·`AuthDtoExpiresInContractTest` 가 가드한다.
 */
class AuthInterceptorTest {
    private var nowElapsedMillis = 0L
    private val tracker = AccessTokenExpiryTracker { nowElapsedMillis }

    private fun interceptor(repository: FakeAuthRepository) =
        AuthInterceptor(
            authRepository = { repository },
            expiryTracker = tracker,
            tokenReissuer = TokenReissuer({ repository }, tracker, FakeErrorReporter()),
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
        // 회전 묶음의 expiresIn 이 null 이라 deadline 은 비워졌다 — 임박 판정이 남지 않아야 한다
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
    fun `선제 reissue 가 refresh 거절 - 락 안에서 세션 정리까지 끝난다`() {
        tracker.record(expiresInSeconds = 30)
        val repository =
            FakeAuthRepository(
                accessToken = "stored-token",
                onRotateToken = {
                    Result.failure(HttpException(RetrofitResponse.error<Unit>(400, "".toResponseBody())))
                },
                onClearSession = { Result.success(Unit) },
            )
        val chain = RecordingChain()

        interceptor(repository).intercept(chain)

        // 되돌릴 수 없는 실패라 어느 경로가 먼저 만나든 결론이 같다 — 정리를 미루면
        // 그 창으로 중복 재발급이 빠져나간다 (#1126).
        assertEquals(1, repository.clearSessionCallCount)
        // 이 요청은 그대로 나가고(서버가 거부한다), 저장소가 비었으므로 다음 요청은 미부착 분기로 간다.
        assertEquals("Bearer stored-token", chain.sentRequest?.header("Authorization"))
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
}

/** [proceed] 에 들어온 요청을 캡처하고 단순 200 봉투를 돌려주는 가짜 chain. */
private class RecordingChain : Interceptor.Chain {
    var sentRequest: Request? = null
        private set

    override fun request(): Request = Request.Builder().url("https://afternote.kro.kr/api/v1/test").build()

    override fun proceed(request: Request): Response {
        sentRequest = request
        return Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("""{"status":200,"code":200,"message":"성공","data":null}""".toResponseBody("application/json".toMediaType()))
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

    // OkHttp 5.4 부터 Chain 이 클라이언트 설정 전체를 노출한다. 이 fake 는 요청 캡처만
    // 담당하므로 전부 미사용 스텁 — with* 는 기존 withConnectTimeout 패턴, 조회는 call() 패턴.
    override val followSslRedirects: Boolean get() = error("not used")

    override val followRedirects: Boolean get() = error("not used")

    override val dns: Dns get() = error("not used")

    override val socketFactory: SocketFactory get() = error("not used")

    override val retryOnConnectionFailure: Boolean get() = error("not used")

    override val authenticator: Authenticator get() = error("not used")

    override val cookieJar: CookieJar get() = error("not used")

    override val cache: Cache? get() = null

    override val proxy: Proxy? get() = null

    override val proxySelector: ProxySelector get() = error("not used")

    override val proxyAuthenticator: Authenticator get() = error("not used")

    override val sslSocketFactoryOrNull: SSLSocketFactory? get() = null

    override val x509TrustManagerOrNull: X509TrustManager? get() = null

    override val hostnameVerifier: HostnameVerifier get() = error("not used")

    override val certificatePinner: CertificatePinner get() = error("not used")

    override val connectionPool: ConnectionPool get() = error("not used")

    override val eventListener: EventListener get() = error("not used")

    override fun withDns(dns: Dns): Interceptor.Chain = this

    override fun withSocketFactory(socketFactory: SocketFactory): Interceptor.Chain = this

    override fun withRetryOnConnectionFailure(retryOnConnectionFailure: Boolean): Interceptor.Chain = this

    override fun withAuthenticator(authenticator: Authenticator): Interceptor.Chain = this

    override fun withCookieJar(cookieJar: CookieJar): Interceptor.Chain = this

    override fun withCache(cache: Cache?): Interceptor.Chain = this

    override fun withProxy(proxy: Proxy?): Interceptor.Chain = this

    override fun withProxySelector(proxySelector: ProxySelector): Interceptor.Chain = this

    override fun withProxyAuthenticator(proxyAuthenticator: Authenticator): Interceptor.Chain = this

    override fun withSslSocketFactory(
        sslSocketFactory: SSLSocketFactory?,
        x509TrustManager: X509TrustManager?,
    ): Interceptor.Chain = this

    override fun withHostnameVerifier(hostnameVerifier: HostnameVerifier): Interceptor.Chain = this

    override fun withCertificatePinner(certificatePinner: CertificatePinner): Interceptor.Chain = this

    override fun withConnectionPool(connectionPool: ConnectionPool): Interceptor.Chain = this
}
