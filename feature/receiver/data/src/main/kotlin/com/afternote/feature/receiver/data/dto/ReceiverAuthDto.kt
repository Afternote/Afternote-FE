package com.afternote.feature.receiver.data.dto

import com.afternote.feature.afternote.data.mapper.formatDateFromServer
import com.afternote.feature.receiver.domain.model.DeliveryVerification
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceivedRecordBox
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
    @SerialName("contentLength") val contentLength: Long,
)

@Serializable
data class ReceiverAuthPresignedUrlDto(
    @SerialName("presignedUrl") val presignedUrl: String,
    @SerialName("fileKey") val fileKey: String,
    @SerialName("fileUrl") val fileUrl: String,
    @SerialName("contentType") val contentType: String,
    @SerialName("contentLength") val contentLength: Long,
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

/**
 * `receiver-auth/record-boxes` 응답 — 서버는 목록을 `recordBoxes` 로 한 번 감싸 내려준다.
 *
 * 기록함이 하나도 없어도 서버는 빈 배열을 담아 보내므로 기본값을 두지 않는다. 기본값을 두면 서버가
 * 키를 빼거나 이름을 바꿔도 «기록함 0건» 으로 조용히 성공해 계약 누락이 은폐된다.
 */
@Serializable
data class ReceivedRecordBoxListDto(
    @SerialName("recordBoxes") val recordBoxes: List<ReceivedRecordBoxDto>,
)

/**
 * 받은 기록함 한 칸.
 *
 * 서버가 주는 필드를 전부 적어 계약을 코드에 고정한다 — `ReceivedRecordBoxContractTest` 가 이 형태를
 * 지킨다. 도메인으로 옮기는 건 지금 소비하는 것뿐이고, 나머지(`receiverName`·`relation`·
 * `recordStatus`·`viewStatus`)는 목록 화면이 이 API 로 옮겨갈 때(#607) 함께 올린다.
 *
 * **nullable 은 BE 실코드로 판정한 것만 둔다.** OpenAPI 에 `requiredMode` 표기가 없어 문서만으로는
 * 전 필드가 optional 로 보이지만(BE#269), 실제 계약은 이렇다.
 *
 * - `receiverName` — `Receiver.name` 이 `@Column(nullable = false)`, 서버가 비울 수 없다.
 * - `recordStatus` — `determineRecordStatus()` 가 `STORED`·`EMPTY` 중 하나를 항상 반환한다.
 * - `viewStatus` — `determineViewStatus()` 가 `VIEWABLE`·`PENDING`·`REQUESTABLE` 중 하나를 항상 반환한다.
 * - `relation` — `@Column(length = 50)` 로 DB 가 null 을 허용한다.
 * - `verificationStatus`·`requestedAt` — 열람 신청이 없으면 통째로 null 이다.
 *   (`verificationStatus` 가 null 이면 [DeliveryVerificationStatus.UNKNOWN] 이 된다.)
 * - `approvedAt` — 승인 상태에서만 채워진다.
 */
@Serializable
data class ReceivedRecordBoxDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("accessCode") val accessCode: String,
    @SerialName("senderName") val senderName: String,
    @SerialName("receiverName") val receiverName: String,
    @SerialName("relation") val relation: String? = null,
    @SerialName("recordStatus") val recordStatus: String,
    @SerialName("viewStatus") val viewStatus: String,
    @SerialName("verificationStatus") val verificationStatus: String? = null,
    @SerialName("requestedAt") val requestedAt: String? = null,
    @SerialName("approvedAt") val approvedAt: String? = null,
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
        contentLength = contentLength,
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

fun ReceivedRecordBoxDto.toDomain(): ReceivedRecordBox =
    ReceivedRecordBox(
        receiverId = receiverId,
        accessCode = accessCode,
        senderName = senderName,
        verificationStatus = verificationStatus?.let(DeliveryVerificationStatus::fromRaw) ?: DeliveryVerificationStatus.UNKNOWN,
        requestedAt = requestedAt,
        approvedAt = approvedAt,
    )

fun ReceiverMessageDto.toDomain(): SenderMessageInfo =
    SenderMessageInfo(
        senderName = senderName,
        message = message,
        createdAt = createdAt?.let(::formatDateFromServer),
    )
