package com.afternote.feature.receiver.data.dto

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
data class ReceiverAuthVerifyRequestDto(
    @SerialName("authCode") val authCode: String,
)

@Serializable
data class ReceiverAuthVerifyDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("receiverName") val receiverName: String,
    @SerialName("senderName") val senderName: String,
    @SerialName("relation") val relation: String,
)

@Serializable
data class ReceiverAuthCodeEmailSendRequestDto(
    @SerialName("email") val email: String,
)

@Serializable
data class ReceiverEmailAuthVerifyRequestDto(
    @SerialName("email") val email: String,
    @SerialName("authCode") val authCode: String,
)

/**
 * `receiver-auth/email/verify` 성공 응답.
 *
 * 서버가 함께 내려주는 `accessCode`(마스터 키와 동일한 UUID)는 **의도적으로 매핑하지 않는다** —
 * 수신하면 다음 단계인 마스터 키 입력에서 받을 값을 미리 아는 셈이라 그 단계가 무력화된다.
 * 서버 측에서도 제거 예정이므로, 필드를 두지 않아야 제거 시점과 무관하게 파싱이 안전하다
 * (`NetworkModule.provideJson` 의 `ignoreUnknownKeys` 가 잔여 키를 무시). 이슈 #454
 */
@Serializable
data class ReceiverEmailAuthVerifyDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("receiverName") val receiverName: String,
    @SerialName("senderName") val senderName: String,
)

@Serializable
data class ReceiverAuthPresignedUrlRequestDto(
    @SerialName("extension") val extension: String,
)

@Serializable
data class ReceiverAuthPresignedUrlDto(
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
data class DeliveryVerificationRequestDto(
    @SerialName("deathCertificateUrl") val deathCertificateUrl: String? = null,
    @SerialName("familyRelationCertificateUrl") val familyRelationCertificateUrl: String? = null,
)

@Serializable
data class DeliveryVerificationDto(
    @SerialName("id") val id: Long,
    @SerialName("status") val status: String,
    @SerialName("deathCertificateUrl") val deathCertificateUrl: String? = null,
    @SerialName("familyRelationCertificateUrl") val familyRelationCertificateUrl: String? = null,
    @SerialName("adminNote") val adminNote: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class ReceiverMessageDto(
    @SerialName("senderName") val senderName: String,
    @SerialName("message") val message: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

fun ReceiverAuthVerifyDto.toDomain(): ReceiverIdentity =
    ReceiverIdentity(
        receiverId = receiverId,
        receiverName = receiverName,
        senderName = senderName,
        relation = relation,
    )

fun ReceiverEmailAuthVerifyDto.toDomain(): ReceiverEmailAuthResult =
    ReceiverEmailAuthResult(
        receiverId = receiverId,
        receiverName = receiverName,
        senderName = senderName,
    )

fun ReceiverAuthPresignedUrlDto.toDomain(): ReceiverAuthPresignedUrl =
    ReceiverAuthPresignedUrl(
        presignedUrl = presignedUrl,
        fileKey = fileKey,
        fileUrl = fileUrl,
        contentType = contentType,
    )

fun DeliveryVerificationDto.toDomain(): DeliveryVerification =
    DeliveryVerification(
        id = id,
        status = DeliveryVerificationStatus.fromRaw(status),
        deathCertificateUrl = deathCertificateUrl,
        familyRelationCertificateUrl = familyRelationCertificateUrl,
        adminNote = adminNote,
        createdAt = createdAt,
    )

fun ReceiverMessageDto.toDomain(): SenderMessageInfo =
    SenderMessageInfo(
        senderName = senderName,
        message = message,
        createdAt = createdAt?.let(::formatDateFromServer),
    )
