package com.afternote.feature.receiver.data.dto

import com.afternote.feature.receiver.data.reporting.RecordingErrorReporter
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `ReceiverAuthDto` 의 toDomain 확장 회귀 가드.
 * 단순 전달 매핑(SenderMessageInfo·ReceiverIdentity·ReceiverAuthPresignedUrl)과
 * SenderMessageInfo 의 createdAt 표시 형식 변환,
 * 핵심 로직인 [DeliveryVerificationStatus.fromWireOrNull] 의 대소문자 무시와,
 * 모르는 값을 UNKNOWN 으로 두되 텔레메트리에 남기는 정책(#1554)을 검증.
 */
class ReceiverAuthDtoMapperTest {
    private val reporter = RecordingErrorReporter()

    @Test
    fun `ReceiverMessageDto toDomain - SenderMessageInfo 매핑`() {
        val result =
            ReceiverMessageDto(
                senderName = "홍길동",
                message = "보고싶다",
                createdAt = "2026-06-08T12:00:53",
            ).toDomain()
        assertEquals("홍길동", result.senderName)
        assertEquals("보고싶다", result.message)
        assertEquals("2026.06.08", result.createdAt)
    }

    @Test
    fun `ReceiverMessageDto toDomain - message·createdAt 미제공 시 null 유지`() {
        val result = ReceiverMessageDto(senderName = "홍길동").toDomain()
        assertNull(result.message)
        assertNull(result.createdAt)
    }

    @Test
    fun `ReceiverAuthVerifyDto toDomain - ReceiverIdentity 매핑`() {
        val result =
            ReceiverAuthVerifyDto(
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
    fun `ReceiverEmailAuthVerifyDto toDomain - ReceiverEmailAuthResult 매핑`() {
        val result =
            ReceiverEmailAuthVerifyDto(
                receiverId = 7L,
                receiverName = "김수신",
                senderName = "홍발신",
            ).toDomain()

        assertEquals(7L, result.receiverId)
        assertEquals("김수신", result.receiverName)
        assertEquals("홍발신", result.senderName)
    }

    @Test
    fun `ReceiverAuthPresignedUrlDto toDomain - 매핑`() {
        val result =
            ReceiverAuthPresignedUrlDto(
                presignedUrl = "url",
                fileKey = "key",
                fileUrl = "file",
                contentType = "image/jpeg",
                contentLength = 123L,
            ).toDomain()

        assertEquals("url", result.presignedUrl)
        assertEquals("key", result.fileKey)
        assertEquals("file", result.fileUrl)
        assertEquals("image/jpeg", result.contentType)
        assertEquals(123L, result.contentLength)
    }

    @Test
    fun `DeliveryVerificationDto toDomain - status 대소문자 무시`() {
        assertEquals(DeliveryVerificationStatus.APPROVED, response(status = "approved").toDomain(reporter).status)
        assertEquals(DeliveryVerificationStatus.PENDING, response(status = "PENDING").toDomain(reporter).status)
    }

    @Test
    fun `DeliveryVerificationDto toDomain - 알 수 없는 status는 UNKNOWN`() {
        assertEquals(DeliveryVerificationStatus.UNKNOWN, response(status = "WeIrD").toDomain(reporter).status)
    }

    /**
     * 서버가 상태를 하나 추가하면 화면은 「아직 신청 안 함」으로 그려진다 — 조용히 두면 아무도 모른다.
     * 화면은 그대로 두되 사실은 남긴다는 것이 #1554 의 결정이다.
     */
    @Test
    fun `DeliveryVerificationDto toDomain - 알 수 없는 status 는 텔레메트리에 남는다`() {
        response(status = "EXPIRED").toDomain(reporter)

        val failure = reporter.failures.single()
        assertEquals("verification_status_mapping", failure.attributes["receiver_stage"])
        assertEquals("EXPIRED", failure.attributes["unknown_status"])
    }

    @Test
    fun `DeliveryVerificationDto toDomain - 나머지 필드 전달`() {
        val result = response(status = "REJECTED").toDomain(reporter)
        assertEquals(11L, result.id)
        assertEquals(DeliveryVerificationStatus.REJECTED, result.status)
        assertEquals("death", result.deathCertificateUrl)
        assertEquals("family", result.familyRelationCertificateUrl)
        assertEquals("note", result.adminNote)
        assertEquals("2025-11-26", result.createdAt)
    }

    private fun response(status: String) =
        DeliveryVerificationDto(
            id = 11L,
            status = status,
            deathCertificateUrl = "death",
            familyRelationCertificateUrl = "family",
            adminNote = "note",
            createdAt = "2025-11-26",
        )
}
