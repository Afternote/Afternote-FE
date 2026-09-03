package com.afternote.feature.afternote.presentation.receiver.recordsbox

import androidx.compose.runtime.Immutable
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus

/**
 * `receiver-auth/record-boxes` 응답으로 구성한 받은 기록함 항목.
 *
 * [recordBoxId]는 받은 기록함 항목을 목록과 상세 화면에서 지정하는 불투명 식별자다.
 */
@Immutable
data class ReceivedRecordItem(
    val recordBoxId: Long,
    val accessCode: String,
    val senderName: String,
    val receiverName: String,
    val relation: String,
    val recordStatus: ReceivedRecordStatus,
    val viewStatus: ReceivedRecordViewStatus,
    val verification: ReceivedRecordVerification,
)
