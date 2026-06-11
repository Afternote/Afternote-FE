package com.afternote.core.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 응답 봉투 `expiresIn` 수신 계약 회귀 가드 (#408).
 *
 * `expiresIn` 은 BE 2026-06-01(커밋 91e2d27a) 도입 — `@IncludeAccessTokenExpiresIn` 이 붙은
 * 목록 endpoint 응답에만 내려오고(2026-06-11 라이브 서버 실측 3599) 그 외 응답엔 키 자체가 없다.
 * 양쪽 모두 디코드가 깨지지 않아야 한다. Json 설정은 `NetworkModule.provideJson` 과 동일.
 */
class BaseResponseExpiresInContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Serializable
    private data class Probe(
        val value: String,
    )

    @Test
    fun `expiresIn 내려주는 목록 endpoint 봉투 - 값 보존 및 data 디코드 정상`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"value":"item"},"expiresIn":3599}"""

        val response = json.decodeFromString<BaseResponse<Probe>>(payload)

        assertEquals(3599L, response.expiresIn)
        assertEquals("item", response.requireData().value)
    }

    @Test
    fun `expiresIn 없는 일반 응답 봉투 - null 유지 및 디코드 정상`() {
        val payload = """{"status":200,"code":200,"message":"성공","data":{"value":"item"}}"""

        val response = json.decodeFromString<BaseResponse<Probe>>(payload)

        assertNull(response.expiresIn)
        assertEquals("item", response.requireData().value)
    }
}
