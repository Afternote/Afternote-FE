package com.afternote.feature.afternote.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `GET /receiver-auth/record-boxes` 응답 계약 회귀 가드 (#607).
 *
 * 페이로드는 2026-08-22 서버 `release`의 `ReceivedRecordBoxResponse` 계약을 바탕으로 합성했다.
 * 프로덕션과 같은 Json 설정으로 envelope 디코드 → `requireData()` → data mapper를 모두 통과시킨다.
 */
class ReceivedRecordBoxContractTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    @Test
    fun `목록 성공 응답 - 카드와 열람 상태가 도메인 모델까지 보존됨`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"recordBoxes":[{"receiverId":18,"accessCode":"bd22c849-0000-4000-8000-000000000000","senderName":"김혜성","receiverName":"김지은","relation":"DAUGHTER","recordStatus":"STORED","viewStatus":"VIEWABLE","verificationStatus":"APPROVED","requestedAt":"2026-07-29T16:58:36","approvedAt":"2026-07-30T04:25:42"}]}}"""

        val result =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .recordBoxes
                .single()
                .toReceivedRecordBox()

        assertEquals(18L, result.receiverId)
        assertEquals("bd22c849-0000-4000-8000-000000000000", result.accessCode)
        assertEquals("김혜성", result.senderName)
        assertEquals("김지은", result.receiverName)
        assertEquals("DAUGHTER", result.relation)
        assertEquals(ReceivedRecordStatus.Stored, result.recordStatus)
        assertEquals(ReceivedRecordViewStatus.Viewable, result.viewStatus)
        assertEquals(DeliveryVerificationStatus.APPROVED, result.verificationStatus)
        assertEquals("2026-07-29T16:58:36", result.requestedAt)
        assertEquals("2026-07-30T04:25:42", result.approvedAt)
    }

    @Test
    fun `미래 enum 값과 nullable 인증 필드 - 디코드 실패 없이 안전한 상태로 매핑됨`() {
        val payload =
            """{"status":200,"code":200,"message":"성공","data":{"recordBoxes":[{"receiverId":19,"accessCode":"bd22c849-0000-4000-8000-000000000001","senderName":"새 발신자","receiverName":"새 수신자","relation":"OTHER","recordStatus":"ARCHIVED","viewStatus":"LOCKED","verificationStatus":null,"requestedAt":null,"approvedAt":null}]}}"""

        val result =
            json
                .decodeFromString<BaseResponse<ReceivedRecordBoxListDto>>(payload)
                .requireData()
                .recordBoxes
                .single()
                .toReceivedRecordBox()

        assertEquals(ReceivedRecordStatus.Unknown, result.recordStatus)
        assertEquals(ReceivedRecordViewStatus.Unknown, result.viewStatus)
        assertNull(result.verificationStatus)
        assertNull(result.requestedAt)
        assertNull(result.approvedAt)
    }
}
