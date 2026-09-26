package com.afternote.core.data.mapper.user

import com.afternote.core.model.user.ReceiverDetail
import com.afternote.core.network.di.NetworkModule
import com.afternote.core.network.dto.ReceiverDetailDto
import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 수신자 상세 응답 → [ReceiverDetail] 매핑 회귀 가드 (#2155).
 *
 * `UserReceiverRepositoryImpl.getReceiverDetail` 이 하는 일(`requireData().toDomain()`)을 dev 서버 상세 응답
 * 모양의 본문에 그대로 태운다. 수정 화면은 빈 관계를 선택 안 함으로 그리므로 null 관계는 빈 문자열로 옮긴다.
 * 상세 응답에 없는 `authCode` 도 빈 문자열이다.
 */
class ReceiverDetailMapperTest {
    private val json = NetworkModule.provideJson()

    @Test
    fun `상세 응답 - 저장된 이름·관계·연락처·마지막 인사말이 그대로 옮겨진다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"receiverId":14,"name":"QA수신자","relation":"DAUGHTER","phone":"010-1234-5678","email":"receiver@afternote.local","dailyQuestionCount":0,"timeLetterCount":0,"afterNoteCount":0,"message":"마지막 인사말"}}"""

        val receiver = json.decodeFromString<BaseResponse<ReceiverDetailDto>>(payload).requireData().toDomain()

        assertEquals(
            ReceiverDetail(
                receiverId = 14L,
                name = "QA수신자",
                relation = "DAUGHTER",
                phone = "010-1234-5678",
                email = "receiver@afternote.local",
                dailyQuestionCount = 0,
                timeLetterCount = 0,
                afterNoteCount = 0,
                message = "마지막 인사말",
                authCode = "",
            ),
            receiver,
        )
    }

    @Test
    fun `상세 응답 - relation 이 null 이면 빈 관계로 옮겨진다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"receiverId":21,"name":"Admin","relation":null,"phone":null,"email":"admin-receiver@afternote.local","dailyQuestionCount":0,"timeLetterCount":0,"afterNoteCount":0,"message":null}}"""

        val receiver = json.decodeFromString<BaseResponse<ReceiverDetailDto>>(payload).requireData().toDomain()

        assertEquals(
            ReceiverDetail(
                receiverId = 21L,
                name = "Admin",
                relation = "",
                phone = null,
                email = "admin-receiver@afternote.local",
                dailyQuestionCount = 0,
                timeLetterCount = 0,
                afterNoteCount = 0,
                message = null,
                authCode = "",
            ),
            receiver,
        )
    }
}
