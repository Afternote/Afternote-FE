package com.afternote.feature.receiver.presentation.summary

/**
 * 모든 기록 내려받기 UI 이벤트.
 */
sealed interface ReceiverDownloadAllEvent {
    data object ConfirmDownload : ReceiverDownloadAllEvent

    data object DownloadSuccessConsumed : ReceiverDownloadAllEvent

    data object ErrorConsumed : ReceiverDownloadAllEvent
}
