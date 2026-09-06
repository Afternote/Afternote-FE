package com.afternote.core.network.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 발급 응답 `data.expiresIn` 수신 계약 회귀 가드 (#410).
 *
 * BE #410(2026-06-20)으로 `expiresIn`(액세스 토큰 잔여 수명, 초)이 봉투 최상위에서 발급 응답
 * (`/auth/login`·`/auth/reissue`)의 `data` 안으로 이동했다(실측 3600). 발급 DTO 가 이를 디코드해
 * 선제 reissue(#408) deadline 으로 흘려보낼 수 있어야 하고, 서버가 생략하면(과거 호환) null 이어야
 * 한다. Json 설정은 `NetworkModule.provideJson` 과 동일.
 */
class AuthDtoExpiresInContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `로그인 응답 - data 안 expiresIn 디코드`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"accessToken":"a","refreshToken":"r","expiresIn":3600}}"""

        val data = json.decodeFromString<BaseResponse<LoginDto.DefaultLoginDto>>(payload).requireData()

        assertEquals(3600L, data.expiresIn)
        assertEquals("a", data.accessToken)
    }

    @Test
    fun `리이슈 응답 - data 안 expiresIn 디코드`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"accessToken":"a","refreshToken":"r","expiresIn":3600}}"""

        val data = json.decodeFromString<BaseResponse<ReissueDto>>(payload).requireData()

        assertEquals(3600L, data.expiresIn)
        assertEquals("r", data.refreshToken)
    }

    @Test
    fun `expiresIn 생략 - null 유지 및 디코드 정상`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"accessToken":"a","refreshToken":"r"}}"""

        val data = json.decodeFromString<BaseResponse<LoginDto.DefaultLoginDto>>(payload).requireData()

        assertNull(data.expiresIn)
        assertEquals("a", data.accessToken)
    }
}
