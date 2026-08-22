package com.afternote.feature.afternote.data.dto

import com.afternote.core.network.model.BaseResponse
import com.afternote.core.network.model.requireData
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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

        assertEquals(18L, result.recordBoxId)
        assertEquals("bd22c849-0000-4000-8000-000000000000", result.accessCode)
        assertEquals("김혜성", result.senderName)
        assertEquals("김지은", result.receiverName)
        assertEquals("DAUGHTER", result.relation)
        assertEquals(ReceivedRecordStatus.Stored, result.recordStatus)
        assertEquals(ReceivedRecordViewStatus.Viewable, result.viewStatus)
        assertEquals(
            ReceivedRecordVerification.Approved(
                requestedAt = "2026-07-29T16:58:36",
                approvedAt = "2026-07-30T04:25:42",
            ),
            result.verification,
        )
    }

    @Test
    fun `미래 enum 값과 미신청 인증 필드 - 디코드 실패 없이 안전한 상태로 매핑됨`() {
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
        assertEquals(ReceivedRecordVerification.NotRequested, result.verification)
    }

    @Test
    fun `대기와 거절 상태 - 신청일을 필수값으로 정규화`() {
        assertEquals(
            ReceivedRecordVerification.Pending("2026-07-29T16:58:36"),
            mapVerification(status = "PENDING", requestedAt = "2026-07-29T16:58:36"),
        )
        assertEquals(
            ReceivedRecordVerification.Rejected("2026-07-29T16:58:36"),
            mapVerification(status = "REJECTED", requestedAt = "2026-07-29T16:58:36"),
        )
    }

    @Test
    fun `미지원 인증 상태 - Unknown으로 정규화`() {
        assertEquals(
            ReceivedRecordVerification.Unknown,
            mapVerification(status = "FUTURE", requestedAt = "2026-07-29T16:58:36"),
        )
    }

    @Test
    fun `알려진 인증 상태의 잘못된 날짜 조합 - 계약 위반으로 실패`() {
        assertContractViolation(
            expected = VerificationContractViolation.NOT_REQUESTED_WITH_DATES,
            status = null,
            requestedAt = "2026-07-29T16:58:36",
        )
        assertContractViolation(
            expected = VerificationContractViolation.PENDING_DATE_MISMATCH,
            status = "PENDING",
        )
        assertContractViolation(
            expected = VerificationContractViolation.REJECTED_DATE_MISMATCH,
            status = "REJECTED",
            requestedAt = "2026-07-29T16:58:36",
            approvedAt = "2026-07-30T04:25:42",
        )
        assertContractViolation(
            expected = VerificationContractViolation.APPROVED_DATE_MISMATCH,
            status = "APPROVED",
            requestedAt = "2026-07-29T16:58:36",
        )
    }

    private fun assertContractViolation(
        expected: VerificationContractViolation,
        status: String?,
        requestedAt: String? = null,
        approvedAt: String? = null,
    ) {
        val exception =
            assertThrows(ReceivedRecordBoxContractException::class.java) {
                mapVerification(status, requestedAt, approvedAt)
            }
        assertEquals(expected, exception.violation)
    }

    private fun mapVerification(
        status: String?,
        requestedAt: String? = null,
        approvedAt: String? = null,
    ): ReceivedRecordVerification =
        ReceivedRecordBoxDto(
            receiverId = 18L,
            accessCode = "record-key",
            senderName = "김혜성",
            receiverName = "김지은",
            relation = "DAUGHTER",
            recordStatus = "STORED",
            viewStatus = "REQUESTABLE",
            verificationStatus = status,
            requestedAt = requestedAt,
            approvedAt = approvedAt,
        ).toReceivedRecordBox().verification
}
