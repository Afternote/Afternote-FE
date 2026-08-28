package com.afternote.core.network.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** 회원가입 이메일 인증 요청의 JSON 키 회귀 가드 (#1325). */
class AuthEmailVerificationContractTest {
    private val json = Json

    @Test
    fun `이메일 인증 요청 - 두 필드를 정확한 서버 키로 직렬화`() {
        val encoded =
            json
                .encodeToString(
                    VerifyEmailRequestDto.serializer(),
                    VerifyEmailRequestDto(
                        email = "local@example.com",
                        certificateCode = "123456",
                    ),
                ).let(json::parseToJsonElement)
                .jsonObject
        val expected =
            json
                .parseToJsonElement(
                    """
                    {
                      "email": "local@example.com",
                      "certificateCode": "123456"
                    }
                    """.trimIndent(),
                ).jsonObject

        assertEquals(expected, encoded)
    }
}
