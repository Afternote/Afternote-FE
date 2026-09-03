package com.afternote.feature.home.presentation.receiver

/** 수신자 홈 화면 사용자 이벤트. */
sealed interface ReceiverHomeEvent {
    data object Retry : ReceiverHomeEvent

    data object RequestDownload : ReceiverHomeEvent

    data object DismissDownload : ReceiverHomeEvent

    data object ConfirmDownload : ReceiverHomeEvent

    data object ConsumeDownloadResult : ReceiverHomeEvent
}
