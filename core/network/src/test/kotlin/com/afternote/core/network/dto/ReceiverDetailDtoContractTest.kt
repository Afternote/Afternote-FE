package com.afternote.core.network.dto

import com.afternote.core.network.di.NetworkModule
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `GET /users/receivers/{receiverId}` 상세 응답 수신 계약 회귀 가드 (#2155).
 *
 * 본문은 2026-09-23 dev 서버가 receiverId 14·21 에 내려준 모양이다(연락처는 자리표시 값으로 바꿨다).
 * BE `ReceiverDetailResponse` 의 9개 필드뿐이라 `authCode` 가 없고, `relation`·`phone` 은 미입력이면 null 로 온다.
 * 어느 쪽이든 디코드에 실패하면 수신자 수정 화면이 열리지 않는다.
 * Json 설정은 앱이 쓰는 [NetworkModule.provideJson] 그대로다.
 */
class ReceiverDetailDtoContractTest {
    private val json = NetworkModule.provideJson()

    @Test
    fun `실서버 상세 응답 - authCode 없는 9필드 본문이 디코드된다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"receiverId":14,"name":"QA수신자","relation":"DAUGHTER","phone":"010-1234-5678","email":"receiver@afternote.local","dailyQuestionCount":0,"timeLetterCount":0,"afterNoteCount":0,"message":"마지막 인사말"}}"""

        val receiver = json.decodeFromString<BaseResponse<ReceiverDetailDto>>(payload).requireData()

        assertEquals(14L, receiver.receiverId)
        assertEquals("QA수신자", receiver.name)
        assertEquals("DAUGHTER", receiver.relation)
        assertEquals("010-1234-5678", receiver.phone)
        assertEquals("receiver@afternote.local", receiver.email)
        assertEquals("마지막 인사말", receiver.message)
    }

    @Test
    fun `실서버 상세 응답 - relation 이 null 이어도 디코드된다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"receiverId":21,"name":"Admin","relation":null,"phone":null,"email":"admin-receiver@afternote.local","dailyQuestionCount":0,"timeLetterCount":0,"afterNoteCount":0,"message":null}}"""

        val receiver = json.decodeFromString<BaseResponse<ReceiverDetailDto>>(payload).requireData()

        assertEquals(21L, receiver.receiverId)
        assertEquals("Admin", receiver.name)
        assertNull(receiver.relation)
        assertNull(receiver.phone)
    }
}
