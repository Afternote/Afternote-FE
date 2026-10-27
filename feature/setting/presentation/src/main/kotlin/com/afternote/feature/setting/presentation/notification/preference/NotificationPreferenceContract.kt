package com.afternote.feature.setting.presentation.notification.preference

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.setting.domain.NotificationPreferences
import java.time.LocalTime

internal sealed interface NotificationPreferenceIntent : MviIntent {
    data object Retry : NotificationPreferenceIntent

    data class ChangeTime(
        val time: LocalTime,
    ) : NotificationPreferenceIntent

    data object ConsumeError : NotificationPreferenceIntent
}

/**
 * @property preferences 서버 설정. 불러오기 전에는 null 이다 — 기본값을 채워 두면 서버 값을 받기 전에
 *   그 시각이 사용자의 설정처럼 보인다.
 */
internal data class NotificationPreferenceUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val preferences: NotificationPreferences? = null,
    val isSaving: Boolean = false,
    val errorMessage: UiText? = null,
) : UiState

internal sealed interface NotificationPreferenceReducerEvent : ReducerEvent {
    data object Loading : NotificationPreferenceReducerEvent

    data class Loaded(
        val preferences: NotificationPreferences,
    ) : NotificationPreferenceReducerEvent

    data object LoadFailed : NotificationPreferenceReducerEvent

    /** 고른 시각을 먼저 그린다. 저장이 실패하면 [Failed] 가 [previous] 로 되돌린다. */
    data class Saving(
        val preferences: NotificationPreferences,
    ) : NotificationPreferenceReducerEvent

    data class Saved(
        val preferences: NotificationPreferences,
    ) : NotificationPreferenceReducerEvent

    data class Failed(
        val previous: NotificationPreferences,
        val message: UiText,
    ) : NotificationPreferenceReducerEvent

    data object ErrorConsumed : NotificationPreferenceReducerEvent
}
