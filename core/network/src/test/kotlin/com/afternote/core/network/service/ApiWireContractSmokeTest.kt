package com.afternote.core.network.service

import com.afternote.core.network.dto.LoginDto
import com.afternote.core.network.dto.SocialLoginRequestDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockserver.client.MockServerClient
import org.mockserver.matchers.MatchType
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.JsonBody.json
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
    private lateinit var mockClient: MockServerClient
    private lateinit var authService: AuthApiService
    private lateinit var userService: UserApiService

    @Before
    fun setUp() {
        mockClient = MockServerClient(mockServer.host, mockServer.serverPort)
        mockClient.reset()

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

    @After
    fun tearDown() {
        mockClient.close()
    }

    @Test
    fun `social login preserves HTTP route, strict request JSON, and response schema`() =
        runTest {
            mockClient
                .`when`(
                    request()
                        .withMethod("POST")
                        .withPath("/api/v1/auth/social/login")
                        .withBody(
                            json(
                                """{"provider":"KAKAO","accessToken":"provider-token"}""",
                                MatchType.STRICT,
                            ),
                        ),
                ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
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
                        ),
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
        }

    @Test
    fun `authenticated receiver request preserves bearer header and relative API path`() =
        runTest {
            mockClient
                .`when`(
                    request()
                        .withMethod("GET")
                        .withPath("/api/v1/users/receivers")
                        .withHeader("Authorization", "Bearer contract-token"),
                ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """{"status":200,"code":200,"message":"ok","data":[]}""",
                        ),
                )

            val result = userService.getReceivers()

            assertEquals(200, result.status)
            assertTrue(result.data.orEmpty().isEmpty())
        }

    companion object {
        private const val ENABLE_ENV = "RUN_API_CONTRACT_SMOKE"
        private const val MOCKSERVER_VERSION = "5.15.0"
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
        }
    }
}
