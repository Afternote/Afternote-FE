package com.afternote.core.network.dto

import com.afternote.core.network.di.NetworkModule
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `GET /users/receivers` 목록 항목 수신 계약 회귀 가드 (#2105).
 *
 * 항목 하나가 디코드에 실패하면 목록 전체가 날아간다 — 서버가 실제로 내려주는 모양을 그대로 태운다.
 * Json 설정은 앱이 쓰는 [NetworkModule.provideJson] 그대로다.
 */
class ReceiverListDtoContractTest {
    private val json = NetworkModule.provideJson()

    @Test
    fun `실서버 목록 응답 - relation 이 null 인 행이 섞여도 두 건 다 디코드된다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":[{"receiverId":14,"name":"QA수신자","relation":"DAUGHTER"},{"receiverId":21,"name":"Admin","relation":null}]}"""

        val data = json.decodeFromString<BaseResponse<List<ReceiverListDto>>>(payload).requireData()

        assertEquals(listOf(14L, 21L), data.map { it.receiverId })
        assertEquals("DAUGHTER", data.first().relation)
        assertNull(data.last().relation)
    }

    @Test
    fun `실서버 목록 응답 - 항목 3필드만 와도 디코드된다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":[{"receiverId":14,"name":"QA수신자","relation":"DAUGHTER"}]}"""

        val receiver = json.decodeFromString<BaseResponse<List<ReceiverListDto>>>(payload).requireData().single()

        assertEquals(14L, receiver.receiverId)
        assertEquals("QA수신자", receiver.name)
        assertEquals("DAUGHTER", receiver.relation)
    }

    /** 서버가 항목에 모르는 키를 더해도 목록은 그대로 와야 한다 (ignoreUnknownKeys). */
    @Test
    fun `실서버 목록 응답 - 미선언 키가 섞여도 디코드된다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":[{"receiverId":14,"name":"QA수신자","relation":"DAUGHTER","authCode":"AFTER123"}]}"""

        val data = json.decodeFromString<BaseResponse<List<ReceiverListDto>>>(payload).requireData()

        assertEquals(14L, data.single().receiverId)
    }

    @Test
    fun `빈 목록은 그대로 0건이다`() {
        val payload = """{"status":200,"code":200,"message":"성공","data":[]}"""

        val data = json.decodeFromString<BaseResponse<List<ReceiverListDto>>>(payload).requireData()

        assertEquals(emptyList<ReceiverListDto>(), data)
    }
}
