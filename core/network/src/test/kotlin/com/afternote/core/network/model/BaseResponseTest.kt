package com.afternote.core.network.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 봉투 검증 두 함수의 실패 계약 회귀 가드 (#1505).
 *
 * `status != 200` 분기를 한 헬퍼로 합치면서 각 호출의 [ApiException] 이 그대로인지 고정한다.
 * 특히 `fallbackMessage` 는 서버가 `message` 를 주지 않았을 때 실패가 어느 호출에서 왔는지
 * 가르는 유일한 단서라, 하나로 눌리면 Crashlytics 에서 두 경로가 같은 문구로 뭉친다.
 */
class BaseResponseTest {
    @Test
    fun `requireData - 봉투 실패에 서버 message 가 없으면 payload 요구 쪽 진단 문구가 남는다`() {
        val response = BaseResponse<String>(status = 500, code = 5000, message = null, data = null)

        val exception = assertThrows(ApiException::class.java) { response.requireData() }

        assertEquals("알 수 없는 서버 에러가 발생했습니다.", exception.message)
        assertNull(exception.serverMessage)
        assertEquals(500, exception.status)
        assertEquals(5000, exception.code)
    }

    @Test
    fun `requireStatus - 봉투 실패에 서버 message 가 없으면 성공 여부만 보는 쪽 진단 문구가 남는다`() {
        val response = BaseResponse<String>(status = 500, code = 5000, message = null, data = null)

        val exception = assertThrows(ApiException::class.java) { response.requireStatus() }

        assertEquals("요청에 실패했습니다.", exception.message)
        assertNull(exception.serverMessage)
        assertEquals(500, exception.status)
        assertEquals(5000, exception.code)
    }

    @Test
    fun `봉투 실패에 서버 message 가 있으면 두 함수 모두 서버 문구를 우선한다`() {
        val response = BaseResponse<String>(status = 409, code = 4090, message = "이미 등록된 수신인입니다.", data = null)

        val fromRequireData = assertThrows(ApiException::class.java) { response.requireData() }
        val fromRequireStatus = assertThrows(ApiException::class.java) { response.requireStatus() }

        assertEquals("이미 등록된 수신인입니다.", fromRequireData.message)
        assertEquals("이미 등록된 수신인입니다.", fromRequireStatus.message)
        assertEquals("이미 등록된 수신인입니다.", fromRequireData.serverMessage)
        assertEquals("이미 등록된 수신인입니다.", fromRequireStatus.serverMessage)
    }

    @Test
    fun `requireData - 200 인데 payload 가 비면 status 200 을 유지한 채 계약 위반으로 올린다`() {
        val response = BaseResponse<String>(status = 200, code = 200, message = "성공", data = null)

        val exception = assertThrows(ApiException::class.java) { response.requireData() }

        assertEquals("성공했으나 데이터가 비어있습니다.", exception.message)
        // 봉투가 성공이라 말한 사실은 왜곡하지 않는다 — 호출처가 status 대역으로 장애를 가른다.
        assertEquals(200, exception.status)
        // 이 실패는 서버 문구가 아니라 클라이언트 판정이므로 서버 message 를 싣지 않는다.
        assertNull(exception.serverMessage)
    }

    @Test
    fun `정상 봉투는 requireData 가 payload 를 그대로 돌려주고 requireStatus 는 통과한다`() {
        val response = BaseResponse(status = 200, code = 200, message = "성공", data = "payload")

        assertEquals("payload", response.requireData())
        response.requireStatus()
    }
}
