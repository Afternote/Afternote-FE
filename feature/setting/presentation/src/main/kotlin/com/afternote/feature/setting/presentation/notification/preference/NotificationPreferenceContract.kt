package com.afternote.feature.setting.presentation.notification.preference

import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviIntent
import com.afternote.core.ui.mvi.ReducerEvent
import com.afternote.core.ui.mvi.UiState
import com.afternote.feature.setting.domain.NotificationPreferences
import java.time.DayOfWeek
import java.time.LocalTime

internal sealed interface NotificationPreferenceIntent : MviIntent {
    data object Retry : NotificationPreferenceIntent

    data class ChangeEnabled(
        val enabled: Boolean,
    ) : NotificationPreferenceIntent

    data class ChangeTime(
        val time: LocalTime,
    ) : NotificationPreferenceIntent

    data class ToggleWeekday(
        val day: DayOfWeek,
    ) : NotificationPreferenceIntent

    data class ChangeWeeklyGoal(
        val goal: Int,
    ) : NotificationPreferenceIntent

    data object Save : NotificationPreferenceIntent

    data object ConsumeSaved : NotificationPreferenceIntent

    data object ConsumeError : NotificationPreferenceIntent
}

/**
 * @property form 편집 중인 값. 불러오기 전에는 null 이다 — 기본값을 채워 두면 서버 값을 받기 전에
 *   「18:00」 이 사용자의 설정처럼 보인다.
 * @property isSaved 저장 성공 신호. 화면이 한 번 안내하고 소비한다.
 */
internal data class NotificationPreferenceUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val form: NotificationPreferences? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: UiText? = null,
) : UiState

internal sealed interface NotificationPreferenceReducerEvent : ReducerEvent {
    data object Loading : NotificationPreferenceReducerEvent

    data class Loaded(
        val preferences: NotificationPreferences,
    ) : NotificationPreferenceReducerEvent

    data object LoadFailed : NotificationPreferenceReducerEvent

    data class Edited(
        val form: NotificationPreferences,
    ) : NotificationPreferenceReducerEvent

    data object Saving : NotificationPreferenceReducerEvent

    data class Saved(
        val preferences: NotificationPreferences,
    ) : NotificationPreferenceReducerEvent

    data class Failed(
        val message: UiText,
    ) : NotificationPreferenceReducerEvent

    data object SavedConsumed : NotificationPreferenceReducerEvent

    data object ErrorConsumed : NotificationPreferenceReducerEvent
}
