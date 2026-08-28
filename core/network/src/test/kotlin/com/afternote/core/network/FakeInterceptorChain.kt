package com.afternote.core.network

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
import java.net.Proxy
import java.net.ProxySelector
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * 인터셉터 단위 테스트용 가짜 [Interceptor.Chain].
 *
 * 나가는 요청을 [sentRequest] 에 잡아 두고, 돌려줄 응답은 [respond] 가 정한다 — 기본은 성공 봉투다.
 */
internal class FakeInterceptorChain(
    private val url: String = "https://afternote.kro.kr/api/v1/test",
    private val respond: (Request) -> Response = { it.jsonResponse(code = 200, body = OK_ENVELOPE) },
) : Interceptor.Chain {
    var sentRequest: Request? = null
        private set

    override fun request(): Request = Request.Builder().url(url).build()

    override fun proceed(request: Request): Response {
        sentRequest = request
        return respond(request)
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

    // OkHttp 5.4 부터 Chain 이 클라이언트 설정 전체를 노출한다. 이 fake 는 요청 캡처와 응답 주입만
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

/** 이 요청에 대한 응답 한 건. [contentType] 은 게이트웨이가 끼어든 비 JSON 응답을 재현할 때 쓴다. */
internal fun Request.jsonResponse(
    code: Int,
    body: String,
    contentType: String = "application/json",
): Response =
    Response
        .Builder()
        .request(this)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code == 200) "OK" else "Bad Request")
        .body(body.toResponseBody(contentType.toMediaType()))
        .build()

private const val OK_ENVELOPE = """{"status":200,"code":200,"message":"성공","data":null}"""
