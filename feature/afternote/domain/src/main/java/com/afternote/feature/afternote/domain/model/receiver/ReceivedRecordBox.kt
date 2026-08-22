package com.afternote.feature.afternote.domain.model.receiver

import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus

/** 서버가 소유하는 받은 기록함 한 건. wire 문자열 변환은 data mapper가 담당한다. */
data class ReceivedRecordBox(
    val receiverId: Long,
    val accessCode: String,
    val senderName: String,
    val receiverName: String,
    val relation: String,
    val recordStatus: ReceivedRecordStatus,
    val viewStatus: ReceivedRecordViewStatus,
    val verificationStatus: DeliveryVerificationStatus?,
    val requestedAt: String?,
    val approvedAt: String?,
)

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
