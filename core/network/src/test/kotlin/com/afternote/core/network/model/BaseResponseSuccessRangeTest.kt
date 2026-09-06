package com.afternote.core.network.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 봉투 성공 판정 대역 회귀 가드.
 *
 * 봉투 `status` 는 BE 가 `HttpStatus.value()` 를 그대로 실은 값이라 성공 대역은 2xx 전체다.
 * 과거 `status != 200` 조건은 `ApiErrorCallAdapterFactory` 가 HTTP 201·202·204 를 성공으로
 * 통과시킨 직후 봉투 단계에서 그 성공을 실패로 뒤집었다 — 그 회귀를 막는다.
 */
class BaseResponseSuccessRangeTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `201 봉투 - requireData 가 payload 를 그대로 돌려준다`() {
        val payload = """{"status":201,"code":200,"message":"성공","data":"created"}"""

        val data = json.decodeFromString<BaseResponse<String>>(payload).requireData()

        assertEquals("created", data)
    }

    @Test
    fun `204 봉투 - data 없이도 requireStatus 가 통과한다`() {
        val payload = """{"status":204,"code":200,"message":"성공"}"""

        json.decodeFromString<BaseResponse<Unit>>(payload).requireStatus()
    }

    @Test
    fun `성공 대역 경계 - 200 과 299 는 통과하고 199 와 300 은 실패한다`() {
        envelope(200).requireStatus()
        envelope(299).requireStatus()

        listOf(199, 300).forEach { status ->
            val exception =
                assertThrows(ApiException::class.java) {
                    envelope(status).requireStatus()
                }

            assertEquals(status, exception.status)
        }
    }

    @Test
    fun `4xx 봉투 - requireStatus 가 status 와 code 를 보존한 ApiException 을 던진다`() {
        val payload = """{"status":400,"code":1400,"message":"요청 값이 올바르지 않습니다."}"""
        val response = json.decodeFromString<BaseResponse<Unit>>(payload)

        val exception = assertThrows(ApiException::class.java) { response.requireStatus() }

        assertEquals(400, exception.status)
        assertEquals(1400, exception.code)
        assertEquals("요청 값이 올바르지 않습니다.", exception.serverMessage)
    }

    @Test
    fun `2xx 인데 data 가 비면 requireData 는 계약 위반으로 실패한다`() {
        val payload = """{"status":201,"code":200,"message":"성공"}"""
        val response = json.decodeFromString<BaseResponse<String>>(payload)

        val exception = assertThrows(ApiException::class.java) { response.requireData() }

        assertEquals(201, exception.status)
        assertEquals("성공했으나 데이터가 비어있습니다.", exception.message)
    }

    private fun envelope(status: Int) = BaseResponse<Unit>(status = status, code = 200)
}
