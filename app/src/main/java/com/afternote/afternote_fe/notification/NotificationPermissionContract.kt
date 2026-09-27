package com.afternote.afternote_fe.notification

import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState

/** 알림 권한 Effect가 현재 기기에서 요청해야 하는지 소비하는 상태. */
internal data class NotificationPermissionUiState(
    val shouldRequest: Boolean = false,
) : UiState

/** Effect의 lifecycle과 권한 확인 결과를 ViewModel에 전달한다. */
internal sealed interface NotificationPermissionIntent : MviIntent {
    data object ObservationStarted : NotificationPermissionIntent

    data object ObservationStopped : NotificationPermissionIntent

    /** 허용·거부 결과를 받았거나 시스템에서 이미 허용된 것을 확인했다. */
    data object RecordRequest : NotificationPermissionIntent
}

/** 로그인·기기 저장소 관찰 결과. 요청 대상 판정은 기존 두 흐름을 함께 읽는다. */
internal sealed interface NotificationPermissionReducerEvent : ReducerEvent {
    data class EligibilityChanged(
        val shouldRequest: Boolean,
    ) : NotificationPermissionReducerEvent
}
