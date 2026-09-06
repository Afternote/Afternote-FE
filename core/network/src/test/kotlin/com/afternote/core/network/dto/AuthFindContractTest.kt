package com.afternote.core.network.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** 아이디/비밀번호 찾기 API의 필수 wire 필드 회귀 가드 (#423). */
class AuthFindContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `인증번호 발송 응답 - expiresAt 절대시각을 디코드`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"expiresAt":"2026-08-28T03:05:00Z"}}"""

        val data = json.decodeFromString<BaseResponse<FindSendCodeDto>>(payload).requireData()

        assertEquals("2026-08-28T03:05:00Z", data.expiresAt)
    }

    @Test
    fun `인증번호 발송 응답 - expiresAt 누락은 계약 위반으로 실패`() {
        val payload = """{"status":200,"code":200,"message":"성공","data":{}}"""

        assertThrows(SerializationException::class.java) {
            json.decodeFromString<BaseResponse<FindSendCodeDto>>(payload)
        }
    }

    @Test
    fun `비밀번호 찾기 요청 - 네 필드를 정확한 서버 키로 직렬화`() {
        val encoded =
            json
                .encodeToString(
                    PasswordFindRequestDto.serializer(),
                    PasswordFindRequestDto(
                        email = "local@example.com",
                        certificateCode = "123456",
                        newPassword = "NewPass1!",
                        confirmPassword = "NewPass1!",
                    ),
                ).let(json::parseToJsonElement)
                .jsonObject
        val expected =
            json
                .parseToJsonElement(
                    """
                    {
                      "email": "local@example.com",
                      "certificateCode": "123456",
                      "newPassword": "NewPass1!",
                      "confirmPassword": "NewPass1!"
                    }
                    """.trimIndent(),
                ).jsonObject

        assertEquals(expected, encoded)
    }
}
