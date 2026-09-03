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
    val contentLength: Long,
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
        /**
         * 서버 상태 문자열을 도메인 값으로 옮긴다. **아는 값이 아니면 `null`** 이다.
         *
         * 폴백을 여기서 하지 않는 이유는, 모르는 값을 [UNKNOWN] 으로 흡수할지 말지가 도메인이 아니라
         * **와이어를 받는 쪽의 정책**이기 때문이다. 예전 `fromRaw` 는 이 자리에서 조용히 [UNKNOWN] 을
         * 돌려줬고, 그래서 서버가 상태를 하나 추가하면 화면이 「아직 신청 안 함」으로 그려지는 것을
         * 아무도 몰랐다. 지금은 `toDomain(errorReporter)` 가 그 사실을 텔레메트리에 남긴다 (#1554).
         *
         * 대소문자는 무시한다 — 서버가 `APPROVED`·`approved` 를 섞어 보내도 같은 상태다.
         */
        fun fromWireOrNull(value: String): DeliveryVerificationStatus? = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

/**
 * 받은 기록함 한 칸 (`receiver-auth/record-boxes`).
 *
 * 서버는 **마스터 키가 아니라 그 키가 가리키는 수신자의 이메일**을 권한 근거로 삼는다 — 같은 이메일로
 * 등록된 기록함이 여러 발신자 것이어도 한 번에 내려온다. 그래서 특정 발신자의 칸을 고르려면
 * 화면이 들고 있는 마스터 키와 [masterKey] 를 맞춰야 한다.
 *
 * 지금 이 모델을 읽는 곳은 발신자 상세의 승인일 표시 하나다(#612). 목록 화면이 이 API 로 옮겨갈 때
 * (#607) `receiverName`·`relation`·`recordStatus`·`viewStatus` 를 여기에 더한다 — 서버는 이미 준다.
 *
 * @property masterKey 이 칸을 여는 접근 코드. 발신자가 수신자에게 건넨 마스터 키와 같은 값이다.
 * @property requestedAt 열람 신청일. 서버 ISO-8601 raw — 표시 형식 변환은 presentation 책임.
 * @property approvedAt 열람 승인일. **서버는 승인 상태일 때만 채운다** — 신청 전·심사 중·반려는 null.
 */
data class ReceivedRecordBox(
    val receiverId: Long,
    val masterKey: String,
    val senderName: String,
    val verificationStatus: DeliveryVerificationStatus,
    val requestedAt: String?,
    val approvedAt: String?,
)

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
