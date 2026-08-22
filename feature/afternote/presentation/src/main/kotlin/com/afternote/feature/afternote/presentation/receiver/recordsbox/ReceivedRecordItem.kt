package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus

/** `receiver-auth/record-boxes` 응답으로 구성한 받은 기록함 항목. */
@Immutable
data class ReceivedRecordItem(
    val receiverId: Long,
    val accessCode: String,
    val senderName: String,
    val receiverName: String,
    val relation: String,
    val recordStatus: ReceivedRecordStatus,
    val viewStatus: ReceivedRecordViewStatus,
    val verification: ReceivedRecordVerification,
)
