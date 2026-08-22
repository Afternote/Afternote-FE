package com.afternote.feature.afternote.data.dto

import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordBox
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceivedRecordBoxListDto(
    @SerialName("recordBoxes") val recordBoxes: List<ReceivedRecordBoxDto>,
)

@Serializable
data class ReceivedRecordBoxDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("accessCode") val accessCode: String,
    @SerialName("senderName") val senderName: String,
    @SerialName("receiverName") val receiverName: String,
    @SerialName("relation") val relation: String,
    @SerialName("recordStatus") val recordStatus: String,
    @SerialName("viewStatus") val viewStatus: String,
    @SerialName("verificationStatus") val verificationStatus: String? = null,
    @SerialName("requestedAt") val requestedAt: String? = null,
    @SerialName("approvedAt") val approvedAt: String? = null,
)

fun ReceivedRecordBoxDto.toReceivedRecordBox(): ReceivedRecordBox =
    ReceivedRecordBox(
        receiverId = receiverId,
        accessCode = accessCode,
        senderName = senderName,
        receiverName = receiverName,
        relation = relation,
        recordStatus = mapRecordStatus(recordStatus),
        viewStatus = mapViewStatus(viewStatus),
        verificationStatus = mapVerificationStatus(verificationStatus),
        requestedAt = requestedAt,
        approvedAt = approvedAt,
    )

private fun mapRecordStatus(raw: String): ReceivedRecordStatus =
    when {
        raw.equals("STORED", ignoreCase = true) -> ReceivedRecordStatus.Stored
        raw.equals("EMPTY", ignoreCase = true) -> ReceivedRecordStatus.Empty
        raw.equals("DELETED", ignoreCase = true) -> ReceivedRecordStatus.Deleted
        else -> ReceivedRecordStatus.Unknown
    }

private fun mapViewStatus(raw: String): ReceivedRecordViewStatus =
    when {
        raw.equals("VIEWABLE", ignoreCase = true) -> ReceivedRecordViewStatus.Viewable
        raw.equals("PENDING", ignoreCase = true) -> ReceivedRecordViewStatus.Pending
        raw.equals("REQUESTABLE", ignoreCase = true) -> ReceivedRecordViewStatus.Requestable
        else -> ReceivedRecordViewStatus.Unknown
    }

private fun mapVerificationStatus(raw: String?): DeliveryVerificationStatus? =
    when {
        raw == null -> null
        raw.equals("PENDING", ignoreCase = true) -> DeliveryVerificationStatus.PENDING
        raw.equals("APPROVED", ignoreCase = true) -> DeliveryVerificationStatus.APPROVED
        raw.equals("REJECTED", ignoreCase = true) -> DeliveryVerificationStatus.REJECTED
        else -> DeliveryVerificationStatus.UNKNOWN
    }
