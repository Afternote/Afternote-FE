package com.afternote.core.data.mapper.auth

import com.afternote.core.network.dto.PasskeyAuthenticationOptionsDto
import com.afternote.core.network.dto.PasskeyCredentialDescriptorDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 패스키 요청 JSON 조립 회귀 가드 (#764).
 *
 * 이 조립이 어긋나면 시스템 선택기가 아예 뜨지 않거나 서버 검증이 challenge 에서 깨진다.
 * 어느 쪽이든 **실기기에서만** 드러나므로, 키 이름과 값 보존을 여기서 못박는다.
 *
 * 기준은 WebAuthn `PublicKeyCredentialRequestOptions` 이고, 값의 출처는 BE
 * `PasskeyService.authenticateOptions()` 응답이다.
 */
class PasskeyMapperTest {
    private fun optionsDto(allowCredentials: List<PasskeyCredentialDescriptorDto> = emptyList()) =
        PasskeyAuthenticationOptionsDto(
            challenge = "mptGkG6wQGe9xsxKPuhbiQ",
            timeout = 300_000L,
            rpId = "afternote.kro.kr",
            allowCredentials = allowCredentials,
            userVerification = "required",
        )

    @Test
    fun `옵션 조립 - 표준 키 이름으로 전 필드를 싣는다`() {
        val requestJson = PasskeyMapper.toAuthenticationOptions(optionsDto()).requestJson

        val parsed = Json.parseToJsonElement(requestJson).jsonObject
        assertEquals(JsonPrimitive("mptGkG6wQGe9xsxKPuhbiQ"), parsed["challenge"])
        assertEquals(JsonPrimitive(300_000L), parsed["timeout"])
        assertEquals(JsonPrimitive("afternote.kro.kr"), parsed["rpId"])
        assertEquals(JsonPrimitive("required"), parsed["userVerification"])
        assertTrue(parsed.getValue("allowCredentials").jsonArray.isEmpty())
    }

    @Test
    fun `옵션 조립 - challenge 는 한 글자도 바꾸지 않는다`() {
        // base64url 원문이다. 서버가 clientDataJSON 안의 값과 대조해 소비하므로 패딩 하나만
        // 달라져도 검증이 깨진다 — 재인코딩하지 않는지 고정한다.
        val challenge = "abc-_123"
        val requestJson =
            PasskeyMapper.toAuthenticationOptions(optionsDto().copy(challenge = challenge)).requestJson

        assertEquals(JsonPrimitive(challenge), Json.parseToJsonElement(requestJson).jsonObject["challenge"])
    }

    @Test
    fun `옵션 조립 - 서버가 후보를 채우면 그대로 실어 보낸다`() {
        val requestJson =
            PasskeyMapper
                .toAuthenticationOptions(
                    optionsDto(allowCredentials = listOf(PasskeyCredentialDescriptorDto(type = "public-key", id = "abc"))),
                ).requestJson

        val candidates =
            Json
                .parseToJsonElement(requestJson)
                .jsonObject
                .getValue("allowCredentials")
                .jsonArray
        assertEquals(1, candidates.size)
        assertEquals(JsonPrimitive("public-key"), candidates.first().jsonObject["type"])
        assertEquals(JsonPrimitive("abc"), candidates.first().jsonObject["id"])
    }

    @Test
    fun `검증 요청 - assertion 하위 트리를 손대지 않고 credential 로 감싼다`() {
        val assertion =
            """{"id":"cid","rawId":"cid","type":"public-key",""" +
                """"response":{"clientDataJSON":"cdj","authenticatorData":"ad","signature":"sig","userHandle":"uh"}}"""

        val credential = PasskeyMapper.toAuthenticateRequest(assertion).credential

        assertEquals(JsonPrimitive("cid"), credential["id"])
        val response = credential.getValue("response").jsonObject
        assertEquals(JsonPrimitive("cdj"), response["clientDataJSON"])
        assertEquals(JsonPrimitive("sig"), response["signature"])
        assertEquals(JsonPrimitive("uh"), response["userHandle"])
    }

    @Test
    fun `검증 요청 - assertion 이 JSON 객체가 아니면 조용히 보내지 않고 드러낸다`() {
        assertThrows(IllegalArgumentException::class.java) {
            PasskeyMapper.toAuthenticateRequest("\"not-an-object\"")
        }
    }
}
