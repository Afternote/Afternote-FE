package com.afternote.feature.receiver.domain.model

data class ReceiverIdentity(
    val receiverId: Long,
    val receiverName: String,
    val senderName: String,
    val relation: String,
)

/**
 * 수신자 본인 확인 이메일 인증 검증(`receiver-auth/email/verify`) 성공 결과.
 *
 * [ReceiverIdentity] 와 비슷하지만 다른 계약 — `relation` 이 없고 [accessCode] 가 있다 (별개 endpoint).
 *
 * @property accessCode 이후 `X-Auth-Code` 헤더에 쓰는 UUID 접근 코드. 백엔드가 "이메일 인증 성공
 *   = 마스터 키 획득" 으로 설계해 마스터 키와 동일한 값을 돌려준다. 마스터 키 입력 단계의
 *   스킵/자동 채움 여부는 디자인 결정 대기 — 결정 전까지 presentation 은 수신만 하고 사용하지 않는다 (#407).
 */
data class ReceiverEmailAuthResult(
    val receiverId: Long,
    val receiverName: String,
    val senderName: String,
    val accessCode: String,
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
