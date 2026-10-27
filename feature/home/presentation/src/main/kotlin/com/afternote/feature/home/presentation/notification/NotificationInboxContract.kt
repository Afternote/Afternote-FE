package com.afternote.feature.home.presentation.notification

import com.afternote.core.domain.repository.notification.NotificationInboxItem
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

internal sealed interface NotificationInboxIntent : MviIntent {
    /** 화면이 다시 보일 때. 첫 로드와 같은 길이지만 이미 그린 목록은 지우지 않는다. */
    data object Refresh : NotificationInboxIntent

    /** 항목을 눌렀다. 이동은 화면 콜백이 하고, 여기서는 읽음 표시만 미리 바꾼다. */
    data class Open(
        val notificationId: Long,
    ) : NotificationInboxIntent
}

internal data class NotificationInboxUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val items: List<NotificationInboxItem> = emptyList(),
) : UiState

internal sealed interface NotificationInboxReducerEvent : ReducerEvent {
    data object Loading : NotificationInboxReducerEvent

    data class Loaded(
        val items: List<NotificationInboxItem>,
    ) : NotificationInboxReducerEvent

    data object LoadFailed : NotificationInboxReducerEvent

    data class Opened(
        val notificationId: Long,
    ) : NotificationInboxReducerEvent
}

internal sealed interface NotificationBadgeIntent : MviIntent {
    data object Refresh : NotificationBadgeIntent
}

/** @property hasUnread 모르면(조회 전·실패) false — 점은 확인된 미읽음만 찍는다. */
internal data class NotificationBadgeUiState(
    val hasUnread: Boolean = false,
) : UiState

internal sealed interface NotificationBadgeReducerEvent : ReducerEvent {
    data class Counted(
        val unreadCount: Long,
    ) : NotificationBadgeReducerEvent
}
