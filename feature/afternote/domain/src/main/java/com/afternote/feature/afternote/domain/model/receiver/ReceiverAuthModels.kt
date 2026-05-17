package com.afternote.feature.afternote.domain.model.receiver

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
    val createdAt: String,
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

data class SenderMessageInfo(
    val senderName: String,
    val message: String,
)
