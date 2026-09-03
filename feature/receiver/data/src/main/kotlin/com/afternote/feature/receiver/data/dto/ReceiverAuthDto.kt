package com.afternote.feature.receiver.data.dto

import com.afternote.core.common.reporting.ErrorReporter
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
class ReceivedRecordBoxListDto private constructor(
    @SerialName("recordBoxes") private val recordBoxes: List<ReceivedRecordBoxDto>,
) {
    internal fun toDomain(errorReporter: ErrorReporter): List<ReceivedRecordBox> = recordBoxes.map { it.toDomain(errorReporter) }
}

/**
 * 받은 기록함 한 칸.
 *
 * 서버가 주는 필드를 전부 적어 계약을 코드에 고정한다 — `ReceivedRecordBoxContractTest` 가 이 형태를
 * 지킨다. 도메인으로 옮기는 건 지금 소비하는 것뿐이고, 나머지(`receiverName`·`relation`·
 * `recordStatus`·`viewStatus`)는 목록 화면이 이 API 로 옮겨갈 때(#607) 함께 올린다.
 *
 * `accessCode` 는 **서버가 이 응답에서 부르는 이름**이라 DTO 에서는 그대로 받는다 — 같은 값을
 * `GET /users/receivers` 는 `authCode` 로, 헤더는 `X-Auth-Code` 로 부른다(2026-08-30 실측). 앱 안에서는
 * 사용자에게 보이는 말인 «마스터 키» 하나로 모으므로, 이름이 갈리는 것은 이 경계까지다 (#1554).
 *
 * **nullable 은 BE 실코드로 판정한 것만 둔다.** OpenAPI 에 `requiredMode` 표기가 없어 문서만으로는
 * 전 필드가 optional 로 보이지만(BE#269), 실제 계약은 이렇다.
 *
 * - `receiverName` — `Receiver.name` 이 `@Column(nullable = false)`, 서버가 비울 수 없다.
 * - `recordStatus` — `determineRecordStatus()` 가 `STORED`·`EMPTY` 중 하나를 항상 반환한다.
 *   (배포 스키마의 enum 엔 `DELETED` 도 있지만 서버가 채우는 경로가 없다 — BE#269.)
 * - `viewStatus` — `determineViewStatus()` 가 `VIEWABLE`·`PENDING`·`REQUESTABLE` 중 하나를 항상 반환한다.
 * - `relation` — `@Column(length = 50)` 로 DB 가 null 을 허용한다.
 * - `verificationStatus`·`requestedAt` — 열람 신청이 없으면 통째로 null 이다.
 *   (`verificationStatus` 가 null 이면 [DeliveryVerificationStatus.UNKNOWN] 이 된다.)
 * - `approvedAt` — 승인 상태에서만 채워진다.
 */
@Serializable
private data class ReceivedRecordBoxDto(
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

private const val RECEIVER_STAGE_KEY = "receiver_stage"
private const val VERIFICATION_STATUS_STAGE = "verification_status_mapping"
private const val UNKNOWN_STATUS_KEY = "unknown_status"

/** 서버가 도메인에 없는 상태 문자열을 보냈음을 알리는 non-fatal 신호. */
private class VerificationStatusMappingFailure : RuntimeException()

/**
 * 서버 상태 문자열을 도메인 값으로 옮긴다. **모르는 값이면 화면은 그대로 두고 텔레메트리에 남긴다** (#1554).
 *
 * 화면을 실패로 떨어뜨리지 않는 이유는, 서버가 상태를 하나 추가했을 뿐인데 사용자가 화면을 통째로
 * 못 보게 되기 때문이다. 그렇다고 조용히 [DeliveryVerificationStatus.UNKNOWN] 으로 흡수하면
 * 「아직 열람 신청 안 함」으로 그려져 이미 신청한 사용자에게 신청 버튼이 다시 보인다 — 그 사실을
 * 아무도 모르는 것이 종전 동작이었다.
 *
 * `null` 은 신호가 아니다. 열람 신청 전에는 서버가 이 필드를 채우지 않으므로 정상 경로다.
 *
 * 상태 문자열은 서버가 정의한 enum 이름이라 텔레메트리에 담되, **enum 형태일 때만** 담는다. 응답 본문이
 * 통째로 흘러들어 개인정보가 섞이는 경로를 만들지 않기 위함이다.
 */
private fun resolveVerificationStatus(
    raw: String?,
    errorReporter: ErrorReporter,
): DeliveryVerificationStatus {
    if (raw == null) return DeliveryVerificationStatus.UNKNOWN

    DeliveryVerificationStatus.fromWireOrNull(raw)?.let { return it }

    errorReporter.recordFailure(
        throwable = VerificationStatusMappingFailure(),
        attributes =
            buildMap {
                put(RECEIVER_STAGE_KEY, VERIFICATION_STATUS_STAGE)
                if (raw.matches(ENUM_LIKE)) put(UNKNOWN_STATUS_KEY, raw)
            },
    )
    return DeliveryVerificationStatus.UNKNOWN
}

private val ENUM_LIKE = Regex("^[A-Za-z_]{1,32}$")

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

fun DeliveryVerificationDto.toDomain(errorReporter: ErrorReporter): DeliveryVerification =
    DeliveryVerification(
        id = id,
        status = resolveVerificationStatus(status, errorReporter),
        deathCertificateUrl = deathCertificateUrl,
        familyRelationCertificateUrl = familyRelationCertificateUrl,
        adminNote = adminNote,
        createdAt = createdAt,
    )

private fun ReceivedRecordBoxDto.toDomain(errorReporter: ErrorReporter): ReceivedRecordBox =
    ReceivedRecordBox(
        receiverId = receiverId,
        masterKey = accessCode,
        senderName = senderName,
        verificationStatus = resolveVerificationStatus(verificationStatus, errorReporter),
        requestedAt = requestedAt,
        approvedAt = approvedAt,
    )

fun ReceiverMessageDto.toDomain(): SenderMessageInfo =
    SenderMessageInfo(
        senderName = senderName,
        message = message,
        createdAt = createdAt?.let(::formatDateFromServer),
    )
