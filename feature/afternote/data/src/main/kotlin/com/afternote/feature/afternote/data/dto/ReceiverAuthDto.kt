package com.afternote.feature.afternote.data.dto

import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverAuthPresignedUrl
import com.afternote.feature.receiver.domain.model.ReceiverEmailAuthResult
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderMessageInfo
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
data class ReceiverAuthCodeEmailSendRequest(
    @SerialName("email") val email: String,
)

@Serializable
data class ReceiverEmailAuthVerifyRequest(
    @SerialName("email") val email: String,
    @SerialName("authCode") val authCode: String,
)

@Serializable
data class ReceiverEmailAuthVerifyResponse(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("receiverName") val receiverName: String,
    @SerialName("senderName") val senderName: String,
    @SerialName("accessCode") val accessCode: String,
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

/**
 * 사망확인 서류 제출 요청 — 서버 스펙상 두 URL 중 하나 이상 필수 (이슈 #380).
 *
 * 두 필드를 `String? = null` 로 둔 이유: 사망진단서/가족관계증명서 중 하나만 내도 되도록(OR) 완화.
 * `= null` 기본값은 kotlinx `encodeDefaults = false`(`NetworkModule.provideJson` 설정)와 맞물려,
 * 제출하지 않은 슬롯은 페이로드에서 **키 자체가 생략**된다 (`"...":null` 을 명시 전송하지 않음).
 * 이 직렬화 형태(미제출 슬롯 키 생략)는 `DeliveryVerificationContractTest` 가 고정한다.
 */
@Serializable
data class DeliveryVerificationRequest(
    @SerialName("deathCertificateUrl") val deathCertificateUrl: String? = null,
    @SerialName("familyRelationCertificateUrl") val familyRelationCertificateUrl: String? = null,
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
    @SerialName("message") val message: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

fun ReceiverAuthVerifyResponse.toDomain(): ReceiverIdentity =
    ReceiverIdentity(
        receiverId = receiverId,
        receiverName = receiverName,
        senderName = senderName,
        relation = relation,
    )

fun ReceiverEmailAuthVerifyResponse.toDomain(): ReceiverEmailAuthResult =
    ReceiverEmailAuthResult(
        receiverId = receiverId,
        receiverName = receiverName,
        senderName = senderName,
        accessCode = accessCode,
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
        createdAt = createdAt?.let(::formatDateFromServer),
    )
