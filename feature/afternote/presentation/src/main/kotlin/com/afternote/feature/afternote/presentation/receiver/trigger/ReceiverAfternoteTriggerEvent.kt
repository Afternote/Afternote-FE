package com.afternote.feature.afternote.presentation.receiver.trigger

/**
 * 수신자 트리거 화면에서 발생하는 사용자 이벤트.
 */
sealed interface ReceiverAfternoteTriggerEvent {
    data class LoadHomeSummary(
        val authCode: String,
    ) : ReceiverAfternoteTriggerEvent

    data class LoadAfterNotes(
        val authCode: String,
    ) : ReceiverAfternoteTriggerEvent
}
