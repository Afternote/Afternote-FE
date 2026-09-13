package com.afternote.feature.home.presentation.receiver

/** 수신자 홈 화면 사용자 이벤트. */
sealed interface ReceiverHomeEvent {
    data object Retry : ReceiverHomeEvent

    data object RequestDownload : ReceiverHomeEvent

    data object DismissDownload : ReceiverHomeEvent

    data object ConfirmDownload : ReceiverHomeEvent

    /** 오류 팝업의 「다시 시도하기」 — 실패한 그 단계를 다시 건다 (#1737). */
    data object RetryDownload : ReceiverHomeEvent

    data object ConsumeDownloadResult : ReceiverHomeEvent
}
