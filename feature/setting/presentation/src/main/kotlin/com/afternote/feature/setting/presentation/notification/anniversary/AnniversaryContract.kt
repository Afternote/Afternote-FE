package com.afternote.feature.setting.presentation.notification.anniversary

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal sealed interface AnniversaryIntent : MviIntent {
    data object Retry : AnniversaryIntent

    data class Add(
        val receiverId: Long,
        val month: Int,
        val day: Int,
    ) : AnniversaryIntent

    data class Delete(
        val anniversaryId: Long,
    ) : AnniversaryIntent

    data object ConsumeError : AnniversaryIntent
}

/** 화면에 그릴 기념일 한 줄. 수신인 이름은 수신인 목록에서 찾아 붙인다 — 서버 응답에는 ID 만 있다. */
internal data class AnniversaryItem(
    val id: Long,
    val receiverName: String?,
    val month: Int,
    val day: Int,
)

internal data class AnniversaryReceiver(
    val id: Long,
    val name: String,
)

internal data class AnniversaryUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val items: List<AnniversaryItem> = emptyList(),
    val receivers: List<AnniversaryReceiver> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: UiText? = null,
) : UiState

internal sealed interface AnniversaryReducerEvent : ReducerEvent {
    data object Loading : AnniversaryReducerEvent

    data class Loaded(
        val items: List<AnniversaryItem>,
        val receivers: List<AnniversaryReceiver>,
    ) : AnniversaryReducerEvent

    data object LoadFailed : AnniversaryReducerEvent

    data object Submitting : AnniversaryReducerEvent

    data class Added(
        val item: AnniversaryItem,
    ) : AnniversaryReducerEvent

    data class Deleted(
        val anniversaryId: Long,
    ) : AnniversaryReducerEvent

    data class Failed(
        val message: UiText,
    ) : AnniversaryReducerEvent

    data object ErrorConsumed : AnniversaryReducerEvent
}
