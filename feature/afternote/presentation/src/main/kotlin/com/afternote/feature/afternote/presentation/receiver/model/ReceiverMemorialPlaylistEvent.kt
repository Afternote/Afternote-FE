package com.afternote.feature.afternote.presentation.receiver.model

/**
 * 수신자 추모 플레이리스트 화면에서 발생하는 사용자 이벤트.
 */
sealed interface ReceiverMemorialPlaylistEvent {
    data object ErrorConsumed : ReceiverMemorialPlaylistEvent
}
