package com.afternote.feature.afternote.data.dto

import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordBox
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordStatus
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordVerification
import com.afternote.feature.afternote.domain.model.receiver.ReceivedRecordViewStatus
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
        verification = mapVerification(verificationStatus, requestedAt, approvedAt),
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

private fun mapVerification(
    status: String?,
    requestedAt: String?,
    approvedAt: String?,
): ReceivedRecordVerification {
    if (status == null) {
        if (requestedAt != null || approvedAt != null) {
            throw ReceivedRecordBoxContractException(VerificationContractViolation.NOT_REQUESTED_WITH_DATES)
        }
        return ReceivedRecordVerification.NotRequested
    }

    return when {
        status.equals("PENDING", ignoreCase = true) &&
            !requestedAt.isNullOrBlank() && approvedAt == null -> {
            ReceivedRecordVerification.Pending(requestedAt)
        }

        status.equals("PENDING", ignoreCase = true) -> {
            throw ReceivedRecordBoxContractException(VerificationContractViolation.PENDING_DATE_MISMATCH)
        }

        status.equals("REJECTED", ignoreCase = true) &&
            !requestedAt.isNullOrBlank() && approvedAt == null -> {
            ReceivedRecordVerification.Rejected(requestedAt)
        }

        status.equals("REJECTED", ignoreCase = true) -> {
            throw ReceivedRecordBoxContractException(VerificationContractViolation.REJECTED_DATE_MISMATCH)
        }

        status.equals("APPROVED", ignoreCase = true) &&
            !requestedAt.isNullOrBlank() && !approvedAt.isNullOrBlank() -> {
            ReceivedRecordVerification.Approved(requestedAt, approvedAt)
        }

        status.equals("APPROVED", ignoreCase = true) -> {
            throw ReceivedRecordBoxContractException(VerificationContractViolation.APPROVED_DATE_MISMATCH)
        }

        else -> {
            ReceivedRecordVerification.Unknown
        }
    }
}

internal enum class VerificationContractViolation {
    NOT_REQUESTED_WITH_DATES,
    PENDING_DATE_MISMATCH,
    REJECTED_DATE_MISMATCH,
    APPROVED_DATE_MISMATCH,
}

internal class ReceivedRecordBoxContractException(
    val violation: VerificationContractViolation,
) : RuntimeException("Invalid received record verification contract: $violation")
