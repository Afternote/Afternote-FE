package com.afternote.feature.afternote.data.dto

import com.afternote.feature.afternote.domain.model.receiver.DeliveryVerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `ReceiverAuthDto` 의 toDomain 확장 회귀 가드.
 * 단순 전달 매핑(SenderMessageInfo·ReceiverIdentity·ReceiverAuthPresignedUrl)과,
 * 핵심 로직인 [DeliveryVerificationStatus.fromRaw] 의 대소문자 무시 + UNKNOWN fallback 을 검증.
 */
class ReceiverAuthDtoMapperTest {
    @Test
    fun `ReceiverMessageResponse toDomain - SenderMessageInfo 매핑`() {
        val result = ReceiverMessageResponse(senderName = "홍길동", message = "보고싶다").toDomain()
        assertEquals("홍길동", result.senderName)
        assertEquals("보고싶다", result.message)
    }

    @Test
    fun `ReceiverAuthVerifyResponse toDomain - ReceiverIdentity 매핑`() {
        val result =
            ReceiverAuthVerifyResponse(
                receiverId = 3L,
                receiverName = "김수신",
                senderName = "홍발신",
                relation = "자녀",
            ).toDomain()

        assertEquals(3L, result.receiverId)
        assertEquals("김수신", result.receiverName)
        assertEquals("홍발신", result.senderName)
        assertEquals("자녀", result.relation)
    }

    @Test
    fun `ReceiverAuthPresignedUrlResponse toDomain - 매핑`() {
        val result =
            ReceiverAuthPresignedUrlResponse(
                presignedUrl = "url",
                fileKey = "key",
                fileUrl = "file",
                contentType = "image/jpeg",
            ).toDomain()

        assertEquals("url", result.presignedUrl)
        assertEquals("key", result.fileKey)
        assertEquals("file", result.fileUrl)
        assertEquals("image/jpeg", result.contentType)
    }

    @Test
    fun `DeliveryVerificationResponse toDomain - status fromRaw 대소문자 무시`() {
        assertEquals(DeliveryVerificationStatus.APPROVED, response(status = "approved").toDomain().status)
        assertEquals(DeliveryVerificationStatus.PENDING, response(status = "PENDING").toDomain().status)
    }

    @Test
    fun `DeliveryVerificationResponse toDomain - 알 수 없는 status는 UNKNOWN`() {
        assertEquals(DeliveryVerificationStatus.UNKNOWN, response(status = "WeIrD").toDomain().status)
    }

    @Test
    fun `DeliveryVerificationResponse toDomain - 나머지 필드 전달`() {
        val result = response(status = "REJECTED").toDomain()
        assertEquals(11L, result.id)
        assertEquals(DeliveryVerificationStatus.REJECTED, result.status)
        assertEquals("death", result.deathCertificateUrl)
        assertEquals("family", result.familyRelationCertificateUrl)
        assertEquals("note", result.adminNote)
        assertEquals("2025-11-26", result.createdAt)
    }

    private fun response(status: String) =
        DeliveryVerificationResponse(
            id = 11L,
            status = status,
            deathCertificateUrl = "death",
            familyRelationCertificateUrl = "family",
            adminNote = "note",
            createdAt = "2025-11-26",
        )
}
