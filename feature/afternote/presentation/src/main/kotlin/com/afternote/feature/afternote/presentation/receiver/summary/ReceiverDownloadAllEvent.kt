package com.afternote.feature.afternote.presentation.receiver.summary

/**
 * 모든 기록 내려받기 UI 이벤트.
 */
sealed interface ReceiverDownloadAllEvent {
    data class ConfirmDownload(
        val authCode: String,
    ) : ReceiverDownloadAllEvent

    data object DownloadSuccessConsumed : ReceiverDownloadAllEvent

    data object ErrorConsumed : ReceiverDownloadAllEvent
}
