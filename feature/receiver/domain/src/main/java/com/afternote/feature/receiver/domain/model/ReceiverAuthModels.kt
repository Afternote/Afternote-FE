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
 * [ReceiverIdentity] 와 비슷하지만 다른 계약 — `relation` 이 없다 (별개 endpoint).
 *
 * 이메일 인증은 신원 확인까지만 담당한다. `X-Auth-Code` 에 쓰는 마스터 키는 이 결과가 아니라
 * 마스터 키 입력 화면의 사용자 입력에서 온다.
 */
data class ReceiverEmailAuthResult(
    val receiverId: Long,
    val receiverName: String,
    val senderName: String,
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
