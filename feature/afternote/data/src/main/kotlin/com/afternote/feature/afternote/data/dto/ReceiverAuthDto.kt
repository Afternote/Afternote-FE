package com.afternote.feature.afternote.data.dto

import com.afternote.feature.afternote.domain.model.receiver.DeliveryVerification
import com.afternote.feature.afternote.domain.model.receiver.DeliveryVerificationStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceiverAuthPresignedUrl
import com.afternote.feature.afternote.domain.model.receiver.ReceiverIdentity
import com.afternote.feature.afternote.domain.model.receiver.SenderMessageInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiverAuthVerifyRequest(
    @SerialName("authCode") val authCode: String,
)

@Serializable
data class ReceiverAuthVerifyResponse(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("receiverName") val receiverName: String,
    @SerialName("senderName") val senderName: String,
    @SerialName("relation") val relation: String,
)

@Serializable
data class ReceiverAuthPresignedUrlRequest(
    @SerialName("extension") val extension: String,
)

@Serializable
data class ReceiverAuthPresignedUrlResponse(
    @SerialName("presignedUrl") val presignedUrl: String,
    @SerialName("fileKey") val fileKey: String,
    @SerialName("fileUrl") val fileUrl: String,
    @SerialName("contentType") val contentType: String,
)

@Serializable
data class DeliveryVerificationRequest(
    @SerialName("deathCertificateUrl") val deathCertificateUrl: String,
    @SerialName("familyRelationCertificateUrl") val familyRelationCertificateUrl: String,
)

@Serializable
data class DeliveryVerificationResponse(
    @SerialName("id") val id: Long,
    @SerialName("status") val status: String,
    @SerialName("deathCertificateUrl") val deathCertificateUrl: String? = null,
    @SerialName("familyRelationCertificateUrl") val familyRelationCertificateUrl: String? = null,
    @SerialName("adminNote") val adminNote: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class ReceiverMessageResponse(
    @SerialName("senderName") val senderName: String,
    @SerialName("message") val message: String,
)

fun ReceiverAuthVerifyResponse.toDomain(): ReceiverIdentity =
    ReceiverIdentity(
        receiverId = receiverId,
        receiverName = receiverName,
        senderName = senderName,
        relation = relation,
    )

fun ReceiverAuthPresignedUrlResponse.toDomain(): ReceiverAuthPresignedUrl =
    ReceiverAuthPresignedUrl(
        presignedUrl = presignedUrl,
        fileKey = fileKey,
        fileUrl = fileUrl,
        contentType = contentType,
    )

fun DeliveryVerificationResponse.toDomain(): DeliveryVerification =
    DeliveryVerification(
        id = id,
        status = DeliveryVerificationStatus.fromRaw(status),
        deathCertificateUrl = deathCertificateUrl,
        familyRelationCertificateUrl = familyRelationCertificateUrl,
        adminNote = adminNote,
        createdAt = createdAt,
    )

fun ReceiverMessageResponse.toDomain(): SenderMessageInfo =
    SenderMessageInfo(
        senderName = senderName,
        message = message,
    )
