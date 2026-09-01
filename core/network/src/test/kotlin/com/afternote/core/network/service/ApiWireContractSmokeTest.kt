package com.afternote.core.network.service

import com.afternote.core.network.dto.AppPlatformDto
import com.afternote.core.network.dto.DeletePushTokenRequestDto
import com.afternote.core.network.dto.EmailFindRequestDto
import com.afternote.core.network.dto.FindSendCodeRequestDto
import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.PasswordFindRequestDto
import com.afternote.core.network.dto.RegisterPushTokenRequestDto
import com.afternote.core.network.dto.SocialLoginRequestDto
import com.afternote.core.network.dto.delivery.ConditionStateDto
import com.afternote.core.network.dto.delivery.DeliveryConditionItemRequestDto
import com.afternote.core.network.dto.delivery.DeliveryConditionTypeDto
import com.afternote.core.network.dto.delivery.DeliveryContentTypeDto
import com.afternote.core.network.dto.delivery.InactivityPeriodDto
import com.afternote.core.network.dto.delivery.ReceiverDeliveryConditionUpdateRequestDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
    private lateinit var accountService: AccountApiService
    private lateinit var appVersionService: AppVersionApiService
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
        val publicRetrofit =
            Retrofit
                .Builder()
                .baseUrl("${mockServer.endpoint}/api/v1/")
                .addConverterFactory(
                    wireJson.asConverterFactory("application/json".toMediaType()),
                ).build()
        val authenticatedRetrofit = publicRetrofit.newBuilder().client(okHttpClient).build()

        accountService = publicRetrofit.create(AccountApiService::class.java)
        appVersionService = publicRetrofit.create(AppVersionApiService::class.java)
        authService = publicRetrofit.create(AuthApiService::class.java)
        userService = authenticatedRetrofit.create(UserApiService::class.java)
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
            assertNoAuthorizationHeader("POST", "/api/v1/auth/social/login")
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

    @Test
    fun `push token PUT preserves authenticated route and strict request JSON`() =
        runTest {
            installExpectation(
                method = "PUT",
                path = "/api/v1/users/push-tokens",
                requestHeaders = mapOf("Authorization" to "Bearer contract-token"),
                requestBody =
                    wireJson
                        .parseToJsonElement(
                            """{"token":"fcm-token","platform":"ANDROID"}""",
                        ).jsonObject,
                responseBody =
                    """
                    {
                      "status": 200,
                      "code": 200,
                      "message": "ok",
                      "data": {
                        "token": "fcm-token",
                        "platform": "ANDROID",
                        "lastSeenAt": "2026-08-29T20:00:00"
                      }
                    }
                    """.trimIndent(),
            )

            val result =
                userService.registerPushToken(
                    RegisterPushTokenRequestDto(token = "fcm-token", platform = "ANDROID"),
                )

            assertEquals(200, result.status)
            assertEquals("fcm-token", result.data?.token)
            assertExactlyOneRecordedRequest("PUT", "/api/v1/users/push-tokens")
        }

    /** DELETE 가 본문을 싣는 드문 경로다 — `@HTTP(hasBody = true)` 가 빠지면 여기서 잡힌다. */
    @Test
    fun `push token DELETE carries request body on the wire`() =
        runTest {
            installExpectation(
                method = "DELETE",
                path = "/api/v1/users/push-tokens",
                requestHeaders = mapOf("Authorization" to "Bearer contract-token"),
                requestBody =
                    wireJson
                        .parseToJsonElement("""{"token":"fcm-token"}""")
                        .jsonObject,
                responseBody = """{"status":200,"code":200,"message":"ok","data":null}""",
            )

            val result = userService.deletePushToken(DeletePushTokenRequestDto(token = "fcm-token"))

            assertEquals(200, result.status)
            assertExactlyOneRecordedRequest("DELETE", "/api/v1/users/push-tokens")
        }

    @Test
    fun `delivery conditions GET preserves authenticated receiver path and response schema`() =
        runTest {
            installExpectation(
                method = "GET",
                path = "/api/v1/users/me/receivers/77/delivery-conditions",
                requestHeaders = mapOf("Authorization" to "Bearer contract-token"),
                responseBody =
                    """
                    {
                      "status": 200,
                      "code": 200,
                      "message": "ok",
                      "data": {
                        "receiverId": 77,
                        "conditions": [{
                          "contentType": "AFTERNOTE",
                          "conditionType": "INACTIVITY",
                          "inactivityPeriod": "ONE_YEAR",
                          "state": "PENDING_CONFIRMATION",
                          "fulfilled": false,
                          "gracePeriodStartedAt": "2026-08-28T03:05:00",
                          "fulfilledAt": null
                        }]
                      }
                    }
                    """.trimIndent(),
            )

            val result = userService.getReceiverDeliveryConditions(77)
            val data = requireNotNull(result.data)
            val condition = data.conditions.single()

            assertEquals(77L, data.receiverId)
            assertEquals(DeliveryContentTypeDto.AFTERNOTE, condition.contentType)
            assertEquals(DeliveryConditionTypeDto.INACTIVITY, condition.conditionType)
            assertEquals(InactivityPeriodDto.ONE_YEAR, condition.inactivityPeriod)
            assertEquals(ConditionStateDto.PENDING_CONFIRMATION, condition.state)
            assertEquals(false, condition.fulfilled)
            assertEquals("2026-08-28T03:05:00", condition.gracePeriodStartedAt)
            assertEquals(null, condition.fulfilledAt)
            assertExactlyOneRecordedRequest("GET", "/api/v1/users/me/receivers/77/delivery-conditions")
        }

    @Test
    fun `delivery conditions PUT preserves authenticated receiver path strict body and response schema`() =
        runTest {
            installExpectation(
                method = "PUT",
                path = "/api/v1/users/me/receivers/77/delivery-conditions",
                requestBody =
                    wireJson
                        .parseToJsonElement(
                            """
                            {
                              "conditions": [{
                                "contentType": "AFTERNOTE",
                                "conditionType": "INACTIVITY",
                                "inactivityPeriod": "ONE_YEAR"
                              }]
                            }
                            """.trimIndent(),
                        ).jsonObject,
                requestHeaders = mapOf("Authorization" to "Bearer contract-token"),
                responseBody =
                    """
                    {
                      "status": 200,
                      "code": 200,
                      "message": "ok",
                      "data": {
                        "receiverId": 77,
                        "conditions": [{
                          "contentType": "AFTERNOTE",
                          "conditionType": "INACTIVITY",
                          "inactivityPeriod": "ONE_YEAR",
                          "state": "ACTIVE",
                          "fulfilled": false,
                          "gracePeriodStartedAt": null,
                          "fulfilledAt": null
                        }]
                      }
                    }
                    """.trimIndent(),
            )

            val result =
                userService.updateReceiverDeliveryConditions(
                    receiverId = 77,
                    request =
                        ReceiverDeliveryConditionUpdateRequestDto(
                            conditions =
                                listOf(
                                    DeliveryConditionItemRequestDto(
                                        contentType = DeliveryContentTypeDto.AFTERNOTE,
                                        conditionType = DeliveryConditionTypeDto.INACTIVITY,
                                        inactivityPeriod = InactivityPeriodDto.ONE_YEAR,
                                    ),
                                ),
                        ),
                )
            val condition = requireNotNull(result.data).conditions.single()

            assertEquals(ConditionStateDto.ACTIVE, condition.state)
            assertEquals(false, condition.fulfilled)
            assertExactlyOneRecordedRequest("PUT", "/api/v1/users/me/receivers/77/delivery-conditions")
        }

    @Test
    fun `find code preserves public POST strict email body and expiresAt response`() =
        runTest {
            installExpectation(
                method = "POST",
                path = "/api/v1/auth/find/send/code",
                requestBody =
                    wireJson
                        .parseToJsonElement("""{"email":"local@example.com"}""")
                        .jsonObject,
                responseBody =
                    """{"status":200,"code":200,"message":"ok","data":{"expiresAt":"2026-08-28T03:05:00Z"}}""",
            )

            val result = accountService.sendFindCode(FindSendCodeRequestDto("local@example.com"))

            assertEquals("2026-08-28T03:05:00Z", requireNotNull(result.data).expiresAt)
            assertExactlyOneRecordedRequest("POST", "/api/v1/auth/find/send/code")
            assertNoAuthorizationHeader("POST", "/api/v1/auth/find/send/code")
        }

    @Test
    fun `email find preserves public POST strict verification body and account response`() =
        runTest {
            installExpectation(
                method = "POST",
                path = "/api/v1/auth/email/find",
                requestBody =
                    wireJson
                        .parseToJsonElement(
                            """{"email":"local@example.com","certificateCode":"123456"}""",
                        ).jsonObject,
                responseBody =
                    """{"status":200,"code":200,"message":"ok","data":{"name":"테스터","email":"local@example.com"}}""",
            )

            val result =
                accountService.findEmail(
                    EmailFindRequestDto(email = "local@example.com", certificateCode = "123456"),
                )
            val data = requireNotNull(result.data)

            assertEquals("테스터", data.name)
            assertEquals("local@example.com", data.email)
            assertExactlyOneRecordedRequest("POST", "/api/v1/auth/email/find")
            assertNoAuthorizationHeader("POST", "/api/v1/auth/email/find")
        }

    @Test
    fun `password find preserves public POST strict reset body and empty response`() =
        runTest {
            installExpectation(
                method = "POST",
                path = "/api/v1/auth/password/find",
                requestBody =
                    wireJson
                        .parseToJsonElement(
                            """
                            {
                              "email": "local@example.com",
                              "certificateCode": "123456",
                              "newPassword": "NewPass1!",
                              "confirmPassword": "NewPass1!"
                            }
                            """.trimIndent(),
                        ).jsonObject,
                responseBody = """{"status":200,"code":200,"message":"ok","data":null}""",
            )

            val result =
                accountService.findPassword(
                    PasswordFindRequestDto(
                        email = "local@example.com",
                        certificateCode = "123456",
                        newPassword = "NewPass1!",
                        confirmPassword = "NewPass1!",
                    ),
                )

            assertEquals(200, result.status)
            assertEquals(null, result.data)
            assertExactlyOneRecordedRequest("POST", "/api/v1/auth/password/find")
            assertNoAuthorizationHeader("POST", "/api/v1/auth/password/find")
        }

    @Test
    fun `app version preserves public GET exact query and nullable store response`() =
        runTest {
            installExpectation(
                method = "GET",
                path = "/api/v1/app/version",
                requestQueryParameters =
                    mapOf(
                        "platform" to "ANDROID",
                        "versionCode" to "10001",
                    ),
                responseBody =
                    """{"status":200,"code":200,"message":"ok","data":{"updateRequired":false,"latestVersionCode":10001,"storeUrl":null}}""",
            )

            val result = appVersionService.checkVersion(platform = AppPlatformDto.ANDROID, versionCode = 10001)
            val data = requireNotNull(result.data)

            assertEquals(false, data.updateRequired)
            assertEquals(10001, data.latestVersionCode)
            assertEquals(null, data.storeUrl)
            assertExactlyOneRecordedRequest("GET", "/api/v1/app/version")
            assertRecordedQueryExactly(
                method = "GET",
                path = "/api/v1/app/version",
                expected = mapOf("platform" to "ANDROID", "versionCode" to "10001"),
            )
            assertNoAuthorizationHeader("GET", "/api/v1/app/version")
        }

    private fun installExpectation(
        method: String,
        path: String,
        requestBody: JsonElement? = null,
        requestHeaders: Map<String, String> = emptyMap(),
        requestQueryParameters: Map<String, String> = emptyMap(),
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
                    if (requestQueryParameters.isNotEmpty()) {
                        putJsonObject("queryStringParameters") {
                            requestQueryParameters.forEach { (name, value) ->
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
        val recorded = recordedRequests(method, path)

        assertEquals("$method $path must cross the socket exactly once", 1, recorded.size)
    }

    private fun assertRecordedQueryExactly(
        method: String,
        path: String,
        expected: Map<String, String>,
    ) {
        val actual =
            recordedRequests(method, path)
                .single()
                .jsonObject["queryStringParameters"]
                ?.jsonObject
        val expectedJson =
            buildJsonObject {
                expected.forEach { (name, value) ->
                    put(name, buildJsonArray { add(JsonPrimitive(value)) })
                }
            }

        assertEquals(expectedJson, actual)
    }

    private fun assertNoAuthorizationHeader(
        method: String,
        path: String,
    ) {
        val headerNames =
            recordedRequests(method, path)
                .single()
                .jsonObject["headers"]
                ?.jsonObject
                ?.keys
                .orEmpty()

        assertTrue(
            "$method $path must be callable without Authorization",
            headerNames.none { it.equals("Authorization", ignoreCase = true) },
        )
    }

    private fun recordedRequests(
        method: String,
        path: String,
    ): JsonArray {
        val matcher =
            buildJsonObject {
                put("method", method)
                put("path", path)
            }

        return wireJson
            .parseToJsonElement(
                controlPut("/mockserver/retrieve?type=REQUESTS", matcher.toString()),
            ).jsonArray
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
