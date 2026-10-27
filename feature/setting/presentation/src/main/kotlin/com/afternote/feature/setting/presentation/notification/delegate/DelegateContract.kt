package com.afternote.feature.setting.presentation.notification.delegate

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.setting.domain.NotificationDelegate

internal sealed interface DelegateIntent : MviIntent {
    data object Retry : DelegateIntent

    data class Save(
        val name: String,
        val phone: String,
    ) : DelegateIntent

    data object Remove : DelegateIntent

    data object ConsumeDone : DelegateIntent

    data object ConsumeError : DelegateIntent
}

/**
 * @property delegate 서버에 지정된 대리인. 없으면 null 이고 입력칸이 빈 채로 열린다.
 * @property doneMessage 저장·해제 성공 안내. 화면이 한 번 띄우고 소비한다.
 */
internal data class DelegateUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val delegate: NotificationDelegate? = null,
    val isSubmitting: Boolean = false,
    val doneMessage: UiText? = null,
    val errorMessage: UiText? = null,
) : UiState

internal sealed interface DelegateReducerEvent : ReducerEvent {
    data object Loading : DelegateReducerEvent

    data class Loaded(
        val delegate: NotificationDelegate?,
    ) : DelegateReducerEvent

    data object LoadFailed : DelegateReducerEvent

    data object Submitting : DelegateReducerEvent

    data class Done(
        val delegate: NotificationDelegate?,
        val message: UiText,
    ) : DelegateReducerEvent

    data class Failed(
        val message: UiText,
    ) : DelegateReducerEvent

    data object DoneConsumed : DelegateReducerEvent

    data object ErrorConsumed : DelegateReducerEvent
}
