package com.afternote.core.network.calladapter

import com.afternote.core.network.di.NetworkModule
import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.BaseResponse
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.GET
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ApiErrorCallAdapterFactoryTest {
    private val json = NetworkModule.provideJson()

    @Test
    fun `400 응답은 서버 필드를 보존한 RuntimeException으로 전달`() =
        runTest {
            val service =
                serviceResponding {
                    it.response(
                        code = 400,
                        body = """{"status":400,"code":1902,"message":"인증번호가 만료되었습니다."}""",
                    )
                }

            val failure = runCatching { service.fetch() }.exceptionOrNull()

            assertTrue(failure is ApiException)
            failure as ApiException
            assertEquals(400, failure.status)
            assertEquals(1902, failure.code)
            assertEquals("인증번호가 만료되었습니다.", failure.serverMessage)
            assertEquals("인증번호가 만료되었습니다.", failure.message)
            assertTrue(RuntimeException::class.java.isAssignableFrom(ApiException::class.java))
            assertFalse(IOException::class.java.isAssignableFrom(ApiException::class.java))
        }

    @Test
    fun `파싱할 수 없는 5xx 본문은 HTTP 상태와 reason을 fallback으로 사용`() =
        runTest {
            val bodies = listOf("", "<html>gateway</html>", """{"status":503""")

            bodies.forEach { body ->
                val failure =
                    runCatching {
                        serviceResponding { it.response(code = 503, message = "Service Unavailable", body = body) }
                            .fetch()
                    }.exceptionOrNull() as ApiException

                assertEquals(503, failure.status)
                assertEquals(503, failure.code)
                assertNull(failure.serverMessage)
                assertEquals("Service Unavailable", failure.message)
            }
        }

    @Test
    fun `null 또는 blank 서버 문구는 서버 문구로 만들지 않음`() =
        runTest {
            val bodies = listOf("""{"code":1901,"message":null}""", """{"code":1901,"message":"  "}""")

            bodies.forEach { body ->
                val failure =
                    runCatching {
                        serviceResponding { it.response(code = 404, message = "Not Found", body = body) }
                            .fetch()
                    }.exceptionOrNull() as ApiException

                assertEquals(1901, failure.code)
                assertNull(failure.serverMessage)
                assertEquals("Not Found", failure.message)
            }
        }

    @Test
    fun `본문과 HTTP reason이 모두 비면 일반 fallback 문구 사용`() =
        runTest {
            val failure =
                runCatching {
                    serviceResponding { it.response(code = 500, message = "", body = "") }
                        .fetch()
                }.exceptionOrNull() as ApiException

            assertEquals("요청에 실패했습니다.", failure.message)
        }

    @Test
    fun `3xx 응답은 ApiException으로 바꾸지 않고 Retrofit HttpException 유지`() =
        runTest {
            val failure =
                runCatching {
                    serviceResponding { it.response(code = 302, message = "Found", body = "") }
                        .fetch()
                }.exceptionOrNull()

            assertTrue(failure is HttpException)
            assertEquals(302, (failure as HttpException).code())
        }

    @Test
    fun `전송 IOException은 ApiException으로 바꾸지 않음`() =
        runTest {
            val transportFailure = IOException("Unable to resolve host")
            val failure =
                runCatching {
                    serviceResponding { throw transportFailure }.fetch()
                }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertFalse(failure is ApiException)
            assertEquals(transportFailure.message, failure?.message)
        }

    @Test
    fun `2xx 응답은 본문 변환 결과 그대로 반환`() =
        runTest {
            val response =
                serviceResponding {
                    it.response(
                        code = 200,
                        message = "OK",
                        body = """{"status":200,"code":200,"message":"성공","data":null}""",
                    )
                }.fetch()

            assertEquals(200, response.status)
            assertEquals(200, response.code)
        }

    @Test
    fun `동기 Call도 4xx를 ApiException으로 전달`() {
        val call =
            serviceResponding {
                it.response(code = 400, body = """{"code":475,"message":"수신자를 선택해 주세요."}""")
            }.fetchCall()

        val failure = runCatching { call.execute() }.exceptionOrNull()

        assertTrue(failure is ApiException)
        assertEquals(475, (failure as ApiException).code)
    }

    @Test
    fun `Call 수명주기와 request timeout은 원본 호출에 위임`() {
        val service =
            serviceResponding(callTimeoutSeconds = 37) {
                it.response(
                    code = 200,
                    message = "OK",
                    body = """{"status":200,"code":200,"message":"성공","data":null}""",
                )
            }
        val original = service.fetchCall()

        assertEquals("GET", original.request().method)
        assertEquals("/test", original.request().url.encodedPath)
        assertEquals(37_000L, original.timeout().timeoutNanos() / 1_000_000)
        assertFalse(original.isCanceled)

        original.cancel()

        assertTrue(original.isCanceled)
        val clone = original.clone()
        assertTrue(clone !== original)
        assertFalse(clone.isCanceled)
        assertFalse(clone.isExecuted)

        val response = clone.execute()

        assertTrue(clone.isExecuted)
        assertEquals(200, response.body()?.status)
    }

    @Test
    fun `일반 Call enqueue callback은 Retrofit callback executor에서 실행`() {
        val executor =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "retrofit-callback-test")
            }
        try {
            val service =
                serviceResponding(callbackExecutor = executor) {
                    it.response(code = 400, body = """{"code":1902,"message":"인증번호가 만료되었습니다."}""")
                }
            val completed = CountDownLatch(1)
            var callbackThread: String? = null
            var callbackResult: String? = null
            var observedFailure: Throwable? = null

            service.fetchCall().enqueue(
                object : Callback<BaseResponse<Unit>> {
                    override fun onResponse(
                        call: Call<BaseResponse<Unit>>,
                        response: Response<BaseResponse<Unit>>,
                    ) {
                        callbackThread = Thread.currentThread().name
                        callbackResult = "response"
                        completed.countDown()
                    }

                    override fun onFailure(
                        call: Call<BaseResponse<Unit>>,
                        throwable: Throwable,
                    ) {
                        callbackThread = Thread.currentThread().name
                        callbackResult = "failure"
                        observedFailure = throwable
                        completed.countDown()
                    }
                },
            )

            assertTrue("callback timeout", completed.await(5, TimeUnit.SECONDS))
            assertEquals("retrofit-callback-test", callbackThread)
            assertEquals("failure", callbackResult)
            assertTrue(observedFailure is ApiException)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun serviceResponding(
        callTimeoutSeconds: Long? = null,
        callbackExecutor: Executor? = null,
        respond: (Request) -> okhttp3.Response,
    ): TestApiService {
        val clientBuilder = OkHttpClient.Builder()
        callTimeoutSeconds?.let { clientBuilder.callTimeout(it, TimeUnit.SECONDS) }
        val client = clientBuilder.addInterceptor { chain -> respond(chain.request()) }.build()

        val retrofitBuilder =
            Retrofit
                .Builder()
                .baseUrl("https://example.com/")
                .client(client)
                .addCallAdapterFactory(ApiErrorCallAdapterFactory(json))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        callbackExecutor?.let(retrofitBuilder::callbackExecutor)

        return retrofitBuilder
            .build()
            .create()
    }

    private fun Request.response(
        code: Int,
        message: String = "Bad Request",
        body: String,
    ): okhttp3.Response =
        okhttp3.Response
            .Builder()
            .request(this)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
}

private interface TestApiService {
    @GET("test")
    suspend fun fetch(): BaseResponse<Unit>

    @GET("test")
    fun fetchCall(): Call<BaseResponse<Unit>>
}
