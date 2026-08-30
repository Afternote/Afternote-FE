package com.afternote.core.network.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 패스키 인증 wire 계약 회귀 가드 (#764).
 *
 * 기준은 배포 서버 OpenAPI 가 아니라 **BE 실코드**다 — OpenAPI 는 전 필드를 optional 로 보여 줘
 * nullable 판정의 근거가 되지 못한다. `PasskeyService.authenticateOptions()` 는 builder 로
 * 다섯 값을 무조건 채우고 어느 갈래에서도 생략하지 않는다. 그래서 non-null 로 선언했고,
 * 이 테스트가 그 선언과 실제 응답 모양이 어긋나는 순간을 잡는다.
 *
 * 아래 성공 페이로드는 배포 서버 실응답 그대로다(2026-08-30 실측):
 * `{"status":200,…,"data":{"challenge":"mptGkG6wQGe9xsxKPuhbiQ","timeout":300000,
 *   "rpId":"afternote.kro.kr","allowCredentials":[],"userVerification":"required"}}`
 *
 * Json 설정은 `NetworkModule.provideJson` 과 동일.
 */
class PasskeyDtoContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private fun optionsPayload(data: String) = """{"status":200,"code":200,"message":"성공","data":$data}"""

    private val liveResponseData =
        """{"challenge":"mptGkG6wQGe9xsxKPuhbiQ","timeout":300000,""" +
            """"rpId":"afternote.kro.kr","allowCredentials":[],"userVerification":"required"}"""

    @Test
    fun `배포 서버 실응답을 그대로 디코드한다`() {
        val data =
            json
                .decodeFromString<BaseResponse<PasskeyAuthenticationOptionsDto>>(optionsPayload(liveResponseData))
                .requireData()

        assertEquals("mptGkG6wQGe9xsxKPuhbiQ", data.challenge)
        assertEquals(300_000L, data.timeout)
        assertEquals("afternote.kro.kr", data.rpId)
        assertEquals("required", data.userVerification)
        assertTrue(data.allowCredentials.isEmpty())
    }

    @Test
    fun `allowCredentials 에 후보가 실려도 버리지 않는다`() {
        // 지금 서버는 usernameless 라 늘 빈 배열이지만, 후보를 채우기 시작해도 조용히
        // 사라지지 않아야 한다 — 여기서 없어진 후보는 시스템 선택기에서 그대로 없어진다.
        val data =
            json
                .decodeFromString<BaseResponse<PasskeyAuthenticationOptionsDto>>(
                    optionsPayload(
                        """{"challenge":"c","timeout":300000,"rpId":"afternote.kro.kr",""" +
                            """"allowCredentials":[{"type":"public-key","id":"abc"}],"userVerification":"required"}""",
                    ),
                ).requireData()

        assertEquals(1, data.allowCredentials.size)
        assertEquals("public-key", data.allowCredentials.first().type)
        assertEquals("abc", data.allowCredentials.first().id)
    }

    @Test
    fun `challenge 누락 - 디코드 실패로 계약 위반이 드러난다`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<PasskeyAuthenticationOptionsDto>>(
                optionsPayload(
                    """{"timeout":300000,"rpId":"afternote.kro.kr","allowCredentials":[],"userVerification":"required"}""",
                ),
            )
        }
    }

    @Test
    fun `challenge 가 null 이면 흡수하지 않고 실패한다`() {
        // 전역 coerceInputValues 를 걷어냈으므로(#1494) null 은 기본값으로 흡수되지 않는다.
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<PasskeyAuthenticationOptionsDto>>(
                optionsPayload(
                    """{"challenge":null,"timeout":300000,"rpId":"a","allowCredentials":[],"userVerification":"required"}""",
                ),
            )
        }
    }

    @Test
    fun `인증 검증 응답은 기존 로그인 토큰 봉투 그대로다`() {
        // BE PasskeyService.authenticate 가 authService.issueTokens 를 그대로 타서 LoginResponse
        // (accessToken·refreshToken·expiresIn) 를 돌려준다 — 그래서 DefaultLoginDto 를 재사용한다.
        val data =
            json
                .decodeFromString<BaseResponse<LoginDto.DefaultLoginDto>>(
                    """{"status":200,"code":200,"message":"성공","data":{"accessToken":"a","refreshToken":"r","expiresIn":3600}}""",
                ).requireData()

        assertEquals("a", data.accessToken)
        assertEquals("r", data.refreshToken)
        assertEquals(3600L, data.expiresIn)
    }

    @Test
    fun `검증 요청은 assertion 트리를 credential 로 감싸 보낸다`() {
        // 서버는 body.credential 이 있으면 그것을, 없으면 body 자체를 검증기에 넘긴다
        // (PasskeyService.credentialJson). 감싼 쪽이 봉투 내용을 이름으로 말해 준다.
        val assertion =
            Json
                .parseToJsonElement("""{"id":"cid","rawId":"cid","type":"public-key","response":{"signature":"sig"}}""")
                .jsonObject

        val encoded = json.encodeToString(PasskeyAuthenticateRequestDto(credential = assertion))

        val credential =
            Json
                .parseToJsonElement(encoded)
                .jsonObject
                .getValue("credential")
                .jsonObject
        assertEquals(JsonPrimitive("cid"), credential["id"])
        assertEquals(JsonPrimitive("public-key"), credential["type"])
        // 인증기가 서명한 하위 트리도 손대지 않고 그대로 실린다.
        assertEquals(JsonPrimitive("sig"), credential.getValue("response").jsonObject["signature"])
    }
}
