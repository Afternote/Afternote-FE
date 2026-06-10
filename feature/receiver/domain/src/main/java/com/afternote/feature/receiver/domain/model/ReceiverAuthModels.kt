package com.afternote.feature.receiver.domain.model

data class ReceiverIdentity(
    val receiverId: Long,
    val receiverName: String,
    val senderName: String,
    val relation: String,
)

data class ReceiverAuthPresignedUrl(
    val presignedUrl: String,
    val fileKey: String,
    val fileUrl: String,
    val contentType: String,
)

data class DeliveryVerification(
    val id: Long,
    val status: DeliveryVerificationStatus,
    val deathCertificateUrl: String?,
    val familyRelationCertificateUrl: String?,
    val adminNote: String?,
    // 서버 ISO-8601 raw 전달 — 표시 형식 변환은 presentation 책임. SenderMessageInfo.createdAt(표시형)과 규약이 다름에 주의.
    val createdAt: String?,
)

enum class DeliveryVerificationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    UNKNOWN,
    ;

    companion object {
        fun fromRaw(value: String): DeliveryVerificationStatus = entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

/**
 * 고인이 남긴 한 마디 (`receiver-auth/message`) 조회 결과.
 *
 * @property message 메시지 본문 — sender 미작성 시 null (서버 스키마 nullable).
 * @property createdAt 작성일. 서버 ISO-8601 을 data 매퍼에서 `yyyy.MM.dd` 표시 형식으로 변환한 값, 서버 미제공 시 null.
 */
data class SenderMessageInfo(
    val senderName: String,
    val message: String?,
    val createdAt: String?,
)
