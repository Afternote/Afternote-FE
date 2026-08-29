package com.afternote.feature.receiver.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `GET /receiver-auth/record-boxes` 응답 계약 가드 (#612 · #607).
 *
 * 페이로드는 BE `ReceivedRecordBoxResponse`·`ReceivedRecordBoxListResponse`(origin/main) 스키마를 그대로
 * 옮긴 것이다 — 목록이 `recordBoxes` 로 한 번 감싸여 오고, 시각 필드는 BE `LocalDateTime` 직렬화라
 * 타임존 표기가 없다.
 *
 * 프로덕션 경로(`ReceiverAuthRepositoryImpl.getReceivedRecordBoxes`)와 같은 순서로
 * Json 디코드 → `requireData()` → `toDomain()` 을 통과시킨다. Json 설정은 `NetworkModule.provideJson` 과 동일.
 */
class ReceivedRecordBoxContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Test
    fun `승인된 칸은 승인일까지 도메인에 도달한다`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"recordBoxes":[
            |{"receiverId":4,"accessCode":"59c04a15-1f4a-4b2e-9a0c-2f4e8d7b6c31","senderName":"김혜성",
            |"receiverName":"김지은","relation":"DAUGHTER","recordStatus":"STORED","viewStatus":"VIEWABLE",
            |"verificationStatus":"APPROVED","requestedAt":"2026-06-21T03:07:26","approvedAt":"2026-07-29T16:00:10"}]}}
            """.trimMargin().replace("\n", "")

        val boxes = json.decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload).requireData().recordBoxes

        val box = boxes.single().toDomain()
        assertEquals(4L, box.receiverId)
        assertEquals("59c04a15-1f4a-4b2e-9a0c-2f4e8d7b6c31", box.accessCode)
        assertEquals("김혜성", box.senderName)
        assertEquals(DeliveryVerificationStatus.APPROVED, box.verificationStatus)
        assertEquals("2026-06-21T03:07:26", box.requestedAt)
        // 발신자 상세의 '승인일' 이 이 값을 표시한다 (#612).
        assertEquals("2026-07-29T16:00:10", box.approvedAt)
    }

    @Test
    fun `승인 전 칸은 승인일이 비어 있다`() {
        val payload =
            """{"status":200,"code":200,"data":{"recordBoxes":[
            |{"receiverId":7,"accessCode":"c0ffee00-0000-4000-8000-000000000001","senderName":"박경민",
            |"receiverName":"김지은","relation":"FRIEND","recordStatus":"STORED","viewStatus":"PENDING",
            |"verificationStatus":"PENDING","requestedAt":"2026-06-21T03:07:26","approvedAt":null}]}}
            """.trimMargin().replace("\n", "")

        val box =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .recordBoxes
                .single()
                .toDomain()

        assertEquals(DeliveryVerificationStatus.PENDING, box.verificationStatus)
        assertNull(box.approvedAt)
    }

    @Test
    fun `열람 신청 전 칸은 상태·시각이 통째로 없어도 디코드된다`() {
        val payload =
            """{"status":200,"code":200,"data":{"recordBoxes":[
            |{"receiverId":9,"accessCode":"c0ffee00-0000-4000-8000-000000000002","senderName":"이영희",
            |"receiverName":"김지은","relation":"MOTHER","recordStatus":"STORED","viewStatus":"REQUESTABLE"}]}}
            """.trimMargin().replace("\n", "")

        val box =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .recordBoxes
                .single()
                .toDomain()

        assertEquals(DeliveryVerificationStatus.UNKNOWN, box.verificationStatus)
        assertNull(box.requestedAt)
        assertNull(box.approvedAt)
    }

    @Test
    fun `같은 이메일의 다른 발신자 칸이 함께 와도 접근 코드로 갈린다`() {
        val payload =
            """{"status":200,"code":200,"data":{"recordBoxes":[
            |{"receiverId":4,"accessCode":"aaaaaaaa-0000-4000-8000-000000000001","senderName":"김혜성",
            |"verificationStatus":"APPROVED","approvedAt":"2026-07-29T16:00:10"},
            |{"receiverId":5,"accessCode":"bbbbbbbb-0000-4000-8000-000000000002","senderName":"박경민",
            |"verificationStatus":"APPROVED","approvedAt":"2026-08-01T09:00:00"}]}}
            """.trimMargin().replace("\n", "")

        val boxes =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .recordBoxes
                .map { it.toDomain() }

        assertEquals(2, boxes.size)
        val mine = boxes.first { it.accessCode == "bbbbbbbb-0000-4000-8000-000000000002" }
        assertEquals("박경민", mine.senderName)
        assertEquals("2026-08-01T09:00:00", mine.approvedAt)
    }

    @Test
    fun `기록함이 하나도 없으면 빈 목록이다`() {
        val payload = """{"status":200,"code":200,"data":{"recordBoxes":[]}}"""

        val boxes = json.decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload).requireData().recordBoxes

        assertTrue(boxes.isEmpty())
    }
}
