package com.afternote.core.network.di

import com.afternote.core.domain.testing.FakeAuthRepository
import com.afternote.core.model.TokenBundle
import com.afternote.core.network.FakeErrorReporter
import com.afternote.core.network.calladapter.ApiErrorCallAdapterFactory
import com.afternote.core.network.dto.ReissueRequestDto
import com.afternote.core.network.interceptor.AuthInterceptor
import com.afternote.core.network.interceptor.TokenAuthenticator
import com.afternote.core.network.service.TokenApiService
import com.afternote.core.network.token.AccessTokenExpiryTracker
import com.afternote.core.network.token.TokenReissuer
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.create
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 액세스 토큰이 만료된 채 콜드 스타트하면 같은 호스트로 동시에 나간 인증 요청이 전부 401 을 받는다.
 * 이때 재발급까지 끝나고 모든 요청이 새 토큰으로 성공해야 한다.
 *
 * 조립은 운영과 같다. [NetworkModule] 의 base·재발급·메인 클라이언트와 [ServiceModule] 의 재발급
 * Retrofit 을 그대로 쓰고, 기본 주소만 로컬 서버로 바꾼다. 요청 5건도 콜드 스타트 스레드 덤프에
 * 찍힌 경로 그대로다. 주간 리포트는 운영처럼 느린 경로용 파생 클라이언트를 탄다.
 *
 * 서버는 만료 토큰 요청이 [ColdStartServer.expiredBarrier] 수만큼 모일 때까지 401 을 쥐고 있는다.
 * 첫 401 이 재발급을 부르기 전에 요청 전부가 디스패처 실행 칸을 차지하게 해, 운 좋게 먼저 끝난
 * 요청이 칸을 비워 주는 경합을 없애기 위해서다.
 */
class ConcurrentUnauthorizedReissueTest {
    private val closeables = mutableListOf<AutoCloseable>()

    @After
    fun tearDown() {
        closeables.asReversed().forEach { runCatching { it.close() } }
    }

    @Test
    fun `같은 호스트 동시 401 이 5건이면 재발급이 끝나고 모든 요청이 성공한다`() {
        assertColdStartCompletes(paths = COLD_START_PATHS)
    }

    @Test
    fun `같은 호스트 동시 401 이 4건이면 재발급이 끝나고 모든 요청이 성공한다`() {
        assertColdStartCompletes(paths = COLD_START_PATHS.take(4))
    }

    private fun assertColdStartCompletes(paths: List<ColdStartRequest>) {
        val server = ColdStartServer(expiredBarrier = paths.size).also(closeables::add)
        val wiring = ColdStartWiring(server.baseUrl).also(closeables::add)

        val finished = CountDownLatch(paths.size)
        val statuses = ConcurrentLinkedQueue<String>()
        paths.forEach { request ->
            wiring.callFactory.newCall(request.toOkHttp(server.baseUrl)).enqueue(
                object : Callback {
                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use { statuses += "${request.path}=${it.code}" }
                        finished.countDown()
                    }

                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        statuses += "${request.path}=${e::class.simpleName}"
                        finished.countDown()
                    }
                },
            )
        }

        if (!finished.await(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            val dispatcher = wiring.baseClient.dispatcher
            val stalled =
                "요청 ${paths.size}건 중 ${paths.size - finished.count}건만 ${COMPLETION_TIMEOUT_SECONDS}초 안에 끝남. " +
                    "재발급 서버 도달 ${server.reissueHits.get()}회, " +
                    "디스패처 실행 ${dispatcher.runningCallsCount()}건·대기 ${dispatcher.queuedCallsCount()}건, " +
                    "maxRequestsPerHost=${dispatcher.maxRequestsPerHost}"
            // 멈춘 OkHttp 스레드는 데몬이 아니다. 상한을 올려 대기 중인 재발급을 내보내야 테스트 JVM 이
            // 스레드를 남기지 않는다. 풀리는지 여부 자체가 원인이 호스트당 상한이라는 증거다.
            dispatcher.maxRequestsPerHost = UNBLOCK_MAX_REQUESTS_PER_HOST
            val releasedAfterRaise = finished.await(COMPLETION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            fail(
                "$stalled. 상한을 $UNBLOCK_MAX_REQUESTS_PER_HOST 로 올린 뒤 풀림=$releasedAfterRaise, " +
                    "재발급 서버 도달 ${server.reissueHits.get()}회, 응답=$statuses",
            )
        }

        assertEquals("재발급은 한 번만 서버에 도달해야 한다", 1, server.reissueHits.get())
        assertEquals(
            paths.map { "${it.path}=200" }.sorted(),
            statuses.toList().sorted(),
        )
    }

    /** 운영 [NetworkModule]·[ServiceModule] 조립을 그대로 따르되 기본 주소만 로컬 서버로 바꾼 묶음. */
    private class ColdStartWiring(
        baseUrl: String,
    ) : AutoCloseable {
        private val json = NetworkModule.provideJson()
        private val callAdapterFactory = ApiErrorCallAdapterFactory(json)
        private val loggingInterceptor = HttpLoggingInterceptor()
        private val tracker = AccessTokenExpiryTracker { 0L }
        private val reporter = FakeErrorReporter()

        val baseClient: OkHttpClient = NetworkModule.provideBaseOkHttpClient()

        private val tokenApiService: TokenApiService =
            ServiceModule
                .provideRefreshRetrofit(
                    refreshClient = NetworkModule.provideRefreshOkHttpClient(baseClient, loggingInterceptor),
                    json = json,
                    apiErrorCallAdapterFactory = callAdapterFactory,
                ).newBuilder()
                .baseUrl(baseUrl)
                .build()
                .create<TokenApiService>()

        /** `AuthRepositoryImpl.rotateToken` 과 같은 순서: 저장 refresh 로 재발급을 치고 새 토큰을 저장한다. */
        private val repository =
            FakeAuthRepository(
                loggedIn = true,
                accessToken = EXPIRED_ACCESS_TOKEN,
                refreshToken = STORED_REFRESH_TOKEN,
                onRotateToken = {
                    runCatching {
                        val data = checkNotNull(tokenApiService.reissue(ReissueRequestDto(checkNotNull(refreshToken))).data)
                        accessToken = data.accessToken
                        refreshToken = data.refreshToken
                        TokenBundle(data.accessToken, data.refreshToken, data.expiresIn)
                    }
                },
            )

        private val reissuer = TokenReissuer({ repository }, tracker, reporter)

        private val mainClient: OkHttpClient =
            NetworkModule.provideMainOkHttpClient(
                baseClient = baseClient,
                debugInterceptors = emptySet(),
                featureInterceptors = emptySet(),
                loggingInterceptor = loggingInterceptor,
                authInterceptor = AuthInterceptor({ repository }, tracker, reissuer),
                tokenAuthenticator = TokenAuthenticator({ repository }, reissuer, reporter),
            )

        /** 운영 Retrofit 의 호출 팩토리 — 주간 리포트만 느린 경로용 파생 클라이언트로 가른다. */
        val callFactory: Call.Factory =
            NetworkModule
                .provideRetrofit(mainClient, json, callAdapterFactory)
                .newBuilder()
                .baseUrl(baseUrl)
                .build()
                .callFactory()

        override fun close() {
            baseClient.dispatcher.cancelAll()
            baseClient.dispatcher.executorService.shutdown()
            baseClient.connectionPool.evictAll()
        }
    }

    private data class ColdStartRequest(
        val method: String,
        val path: String,
        val jsonBody: String? = null,
    ) {
        fun toOkHttp(baseUrl: String): Request =
            Request
                .Builder()
                .url(baseUrl + path)
                .method(method, jsonBody?.toRequestBody(JSON_MEDIA_TYPE))
                .build()
    }

    /**
     * 만료 토큰에는 401, 재발급에는 새 토큰, 새 토큰에는 200 으로 답하는 최소 HTTP/1.1 서버.
     * 새 테스트 의존성 없이 소켓 경계를 실제로 지나게 하려고 `java.net` 만 쓴다.
     */
    private class ColdStartServer(
        expiredBarrier: Int,
    ) : AutoCloseable {
        private val serverSocket = ServerSocket(0, BACKLOG, InetAddress.getLoopbackAddress())
        private val sockets = ConcurrentLinkedQueue<Socket>()
        private val expiredArrivals = CountDownLatch(expiredBarrier)
        val reissueHits = AtomicInteger()
        val baseUrl = "http://${serverSocket.inetAddress.hostAddress}:${serverSocket.localPort}/api/v1/"

        init {
            daemon("cold-start-server-accept") {
                while (!serverSocket.isClosed) {
                    val socket = runCatching { serverSocket.accept() }.getOrNull() ?: break
                    sockets += socket
                    daemon("cold-start-server-conn") { serve(socket) }
                }
            }
        }

        private fun serve(socket: Socket) {
            socket.use {
                val input = BufferedInputStream(it.getInputStream())
                val output = it.getOutputStream()
                while (true) {
                    val requestLine = input.readHttpLine() ?: return
                    if (requestLine.isEmpty()) continue
                    val headers = generateSequence { input.readHttpLine()?.takeIf(String::isNotEmpty) }.toList()
                    val contentLength =
                        headers
                            .firstOrNull { header -> header.startsWith("Content-Length:", ignoreCase = true) }
                            ?.substringAfter(':')
                            ?.trim()
                            ?.toInt() ?: 0
                    repeat(contentLength) { input.read() }
                    val authorization =
                        headers
                            .firstOrNull { header -> header.startsWith("Authorization:", ignoreCase = true) }
                            ?.substringAfter(':')
                            ?.trim()
                    val (status, body) = respond(requestLine, authorization)
                    val bytes = body.toByteArray()
                    output.write(
                        (
                            "HTTP/1.1 $status ${if (status == 200) "OK" else "Unauthorized"}\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Content-Length: ${bytes.size}\r\n\r\n"
                        ).toByteArray(),
                    )
                    output.write(bytes)
                    output.flush()
                }
            }
        }

        private fun respond(
            requestLine: String,
            authorization: String?,
        ): Pair<Int, String> =
            when {
                requestLine.startsWith("POST /api/v1/auth/reissue ") -> {
                    reissueHits.incrementAndGet()
                    200 to REISSUE_RESPONSE
                }

                authorization == "Bearer $FRESH_ACCESS_TOKEN" -> {
                    200 to OK_RESPONSE
                }

                else -> {
                    expiredArrivals.countDown()
                    expiredArrivals.await(BARRIER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    401 to EXPIRED_RESPONSE
                }
            }

        override fun close() {
            serverSocket.close()
            sockets.forEach { runCatching { it.close() } }
        }
    }

    private companion object {
        const val EXPIRED_ACCESS_TOKEN = "expired-access"
        const val FRESH_ACCESS_TOKEN = "fresh-access"
        const val STORED_REFRESH_TOKEN = "stored-refresh"
        const val COMPLETION_TIMEOUT_SECONDS = 5L
        const val BARRIER_TIMEOUT_SECONDS = 5L
        const val UNBLOCK_MAX_REQUESTS_PER_HOST = 64
        const val BACKLOG = 50

        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /** 콜드 스타트 스레드 덤프(2026-09-23)에서 멈춰 있던 요청 5건. */
        val COLD_START_PATHS =
            listOf(
                ColdStartRequest("GET", "users/me"),
                ColdStartRequest("GET", "users/receivers"),
                ColdStartRequest("GET", "daily-questions/today"),
                ColdStartRequest("PUT", "users/push-tokens", """{"token":"fid","platform":"ANDROID"}"""),
                ColdStartRequest("GET", "mind-record"),
            )

        const val OK_RESPONSE = """{"status":200,"code":200,"message":"ok","data":null}"""
        const val EXPIRED_RESPONSE = """{"status":401,"code":1106,"message":"expired"}"""
        const val REISSUE_RESPONSE =
            """{"status":200,"code":200,"message":"ok",""" +
                """"data":{"accessToken":"$FRESH_ACCESS_TOKEN","refreshToken":"fresh-refresh","expiresIn":3600}}"""

        fun daemon(
            name: String,
            block: () -> Unit,
        ) {
            Thread(block, name).apply { isDaemon = true }.start()
        }

        /** CRLF 로 끝나는 한 줄을 읽는다. 스트림이 닫혔으면 null. */
        fun InputStream.readHttpLine(): String? {
            val line = ByteArrayOutputStream()
            while (true) {
                when (val byte = read()) {
                    -1 -> return if (line.size() == 0) null else line.toString(Charsets.US_ASCII.name())
                    '\n'.code -> return line.toString(Charsets.US_ASCII.name()).trimEnd('\r')
                    else -> line.write(byte)
                }
            }
        }
    }
}
