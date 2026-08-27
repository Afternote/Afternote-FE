package com.afternote.core.network.service

import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.SocialLoginRequestDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.mockserver.MockServerContainer
import org.testcontainers.utility.DockerImageName
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Retrofit 선언과 kotlinx-serialization DTO를 실제 HTTP 소켓 경계까지 통과시키는 smoke 계약.
 *
 * 단순 fixture decode 테스트와 달리 method/path/header/request body가 하나라도 바뀌면 MockServer의
 * strict matcher가 응답하지 않아 실패한다. 실제 운영 서버를 호출하지 않으므로 계정·secret은 필요 없다.
 * Docker가 없는 일반 unit-test 실행에서는 건너뛰고, 전용 Actions workflow가 명시적으로 활성화한다.
 */
class ApiWireContractSmokeTest {
    private lateinit var authService: AuthApiService
    private lateinit var userService: UserApiService

    @Before
    fun setUp() {
        controlPut("/mockserver/reset")

        val okHttpClient =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain
                            .request()
                            .newBuilder()
                            .header("Authorization", "Bearer contract-token")
                            .build(),
                    )
                }.build()
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl("${mockServer.endpoint}/api/v1/")
                .client(okHttpClient)
                .addConverterFactory(
                    wireJson.asConverterFactory("application/json".toMediaType()),
                ).build()

        authService = retrofit.create(AuthApiService::class.java)
        userService = retrofit.create(UserApiService::class.java)
    }

    @Test
    fun `social login preserves HTTP route, strict request JSON, and response schema`() =
        runTest {
            installExpectation(
                method = "POST",
                path = "/api/v1/auth/social/login",
                requestBody =
                    wireJson
                        .parseToJsonElement(
                            """{"provider":"KAKAO","accessToken":"provider-token"}""",
                        ).jsonObject,
                responseBody =
                    """
                    {
                      "status": 200,
                      "code": 200,
                      "message": "ok",
                      "data": {
                        "accessToken": "access",
                        "refreshToken": "refresh",
                        "isNewUser": true,
                        "expiresIn": 3600
                      }
                    }
                    """.trimIndent(),
            )

            val result =
                authService.socialLogin(
                    SocialLoginRequestDto(provider = "KAKAO", accessToken = "provider-token"),
                )
            val data = result.data as LoginDto.SocialLoginDto

            assertEquals(200, result.status)
            assertEquals("access", data.accessToken)
            assertEquals("refresh", data.refreshToken)
            assertTrue(data.isNewUser)
            assertEquals(3600L, data.expiresIn)
            assertExactlyOneRecordedRequest("POST", "/api/v1/auth/social/login")
        }

    @Test
    fun `authenticated receiver request preserves bearer header and relative API path`() =
        runTest {
            installExpectation(
                method = "GET",
                path = "/api/v1/users/receivers",
                requestHeaders = mapOf("Authorization" to "Bearer contract-token"),
                responseBody = """{"status":200,"code":200,"message":"ok","data":[]}""",
            )

            val result = userService.getReceivers()

            assertEquals(200, result.status)
            assertTrue(result.data.orEmpty().isEmpty())
            assertExactlyOneRecordedRequest("GET", "/api/v1/users/receivers")
        }

    private fun installExpectation(
        method: String,
        path: String,
        requestBody: JsonElement? = null,
        requestHeaders: Map<String, String> = emptyMap(),
        responseBody: String,
    ) {
        val expectation =
            buildJsonObject {
                putJsonObject("httpRequest") {
                    put("method", method)
                    put("path", path)
                    if (requestHeaders.isNotEmpty()) {
                        putJsonObject("headers") {
                            requestHeaders.forEach { (name, value) ->
                                put(name, buildJsonArray { add(JsonPrimitive(value)) })
                            }
                        }
                    }
                    if (requestBody != null) {
                        putJsonObject("body") {
                            put("type", "JSON")
                            put("json", requestBody)
                            put("matchType", "STRICT")
                        }
                    }
                }
                putJsonObject("httpResponse") {
                    put("statusCode", 200)
                    putJsonObject("headers") {
                        put(
                            "Content-Type",
                            buildJsonArray { add(JsonPrimitive("application/json")) },
                        )
                    }
                    put("body", responseBody)
                }
            }

        controlPut("/mockserver/expectation", expectation.toString())
    }

    private fun assertExactlyOneRecordedRequest(
        method: String,
        path: String,
    ) {
        val matcher =
            buildJsonObject {
                put("method", method)
                put("path", path)
            }
        val recorded =
            wireJson
                .parseToJsonElement(
                    controlPut("/mockserver/retrieve?type=REQUESTS", matcher.toString()),
                ).jsonArray

        assertEquals("$method $path must cross the socket exactly once", 1, recorded.size)
    }

    private fun controlPut(
        path: String,
        payload: String = "",
    ): String {
        val request =
            Request
                .Builder()
                .url("${mockServer.endpoint}$path")
                .put(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

        return controlClient.newCall(request).execute().use { response ->
            val responseBody = response.body.string()
            check(response.isSuccessful) {
                "MockServer control PUT $path failed: ${response.code} $responseBody"
            }
            responseBody
        }
    }

    companion object {
        private const val ENABLE_ENV = "RUN_API_CONTRACT_SMOKE"
        private const val MOCKSERVER_VERSION = "7.6.0"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val controlClient = OkHttpClient()
        private val wireJson =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

        private lateinit var mockServer: MockServerContainer

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            assumeTrue(
                "$ENABLE_ENV=true인 전용 workflow에서만 Docker 계약 검증을 실행한다",
                System.getenv(ENABLE_ENV) == "true",
            )
            assumeTrue("Docker runtime is required", DockerClientFactory.instance().isDockerAvailable)

            mockServer =
                MockServerContainer(
                    DockerImageName.parse("mockserver/mockserver:mockserver-$MOCKSERVER_VERSION"),
                )
            mockServer.start()
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            if (::mockServer.isInitialized) {
                mockServer.stop()
            }
            controlClient.dispatcher.executorService.shutdown()
            controlClient.connectionPool.evictAll()
        }
    }
}
