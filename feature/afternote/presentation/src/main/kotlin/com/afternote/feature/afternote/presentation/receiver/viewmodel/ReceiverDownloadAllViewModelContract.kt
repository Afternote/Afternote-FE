package com.afternote.feature.afternote.presentation.receiver.viewmodel
import com.afternote.feature.afternote.presentation.receiver.model.uistate.ReceiverDownloadAllUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * 모든 기록 내려받기 ViewModel 계약.
 * Preview에서 Hilt 없이 Fake로 대체할 수 있도록 합니다.
 */
interface ReceiverDownloadAllViewModelContract {
    val uiState: StateFlow<ReceiverDownloadAllUiState>

    fun onEvent(event: ReceiverDownloadAllEvent)
}

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
