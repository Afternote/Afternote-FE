package com.afternote.feature.afternote.domain.model.receiver

/**
 * 서버가 구성한 받은 기록함 한 건. wire 문자열 변환은 data mapper가 담당한다.
 *
 * [recordBoxId]는 받은 기록함 항목을 지정하는 불투명 식별자이며, 서버 내부 엔티티 의미를 노출하지 않는다.
 */
data class ReceivedRecordBox(
    val recordBoxId: Long,
    val accessCode: String,
    val senderName: String,
    val receiverName: String,
    val relation: String,
    val recordStatus: ReceivedRecordStatus,
    val viewStatus: ReceivedRecordViewStatus,
    val verification: ReceivedRecordVerification,
)

/** 받은 기록함의 열람 인증 상태. data mapper가 wire의 nullable 조합을 검증해 이 타입으로 정규화한다. */
sealed interface ReceivedRecordVerification {
    data object NotRequested : ReceivedRecordVerification

    data class Pending(
        val requestedAt: String,
    ) : ReceivedRecordVerification

    data class Rejected(
        val requestedAt: String,
    ) : ReceivedRecordVerification

    data class Approved(
        val requestedAt: String,
        val approvedAt: String,
    ) : ReceivedRecordVerification

    /** 앱이 아직 지원하지 않는 서버 인증 상태. */
    data object Unknown : ReceivedRecordVerification
}

enum class ReceivedRecordStatus {
    Stored,
    Empty,
    Deleted,
    Unknown,
}

enum class ReceivedRecordViewStatus {
    Viewable,
    Pending,
    Requestable,
    Unknown,
}
