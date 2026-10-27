package com.afternote.feature.setting.presentation.notification.preference

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.NotificationPreferenceRepository
import com.afternote.feature.setting.domain.NotificationPreferences
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 기록 알림 시각·요일·주간 목표를 고친다 (#2321).
 *
 * 값을 바꿀 때마다 저장하지 않고 «저장» 으로 한 번에 보낸다. 서버는 설정이 바뀔 때마다 대기 중인
 * 기록 알림을 취소하고 다시 잡으므로, 시각을 고르는 도중의 값마다 그 일을 시키지 않는다.
 */
@HiltViewModel
internal class NotificationPreferenceViewModel
    @Inject
    constructor(
        private val repository: NotificationPreferenceRepository,
    ) : MviViewModel<NotificationPreferenceIntent, NotificationPreferenceUiState, NotificationPreferenceReducerEvent>(
            NotificationPreferenceUiState(),
        ) {
        init {
            load()
        }

        override fun onIntent(intent: NotificationPreferenceIntent) {
            when (intent) {
                NotificationPreferenceIntent.Retry -> load()
                is NotificationPreferenceIntent.ChangeEnabled -> edit { it.copy(enabled = intent.enabled) }
                is NotificationPreferenceIntent.ChangeTime -> edit { it.copy(time = intent.time.withSecond(0).withNano(0)) }
                is NotificationPreferenceIntent.ToggleWeekday -> edit { it.copy(weekdays = it.weekdays.toggle(intent.day)) }
                is NotificationPreferenceIntent.ChangeWeeklyGoal -> edit { it.copy(weeklyGoal = intent.goal.coerceIn(MIN_GOAL, MAX_GOAL)) }
                NotificationPreferenceIntent.Save -> save()
                NotificationPreferenceIntent.ConsumeSaved -> dispatch(NotificationPreferenceReducerEvent.SavedConsumed)
                NotificationPreferenceIntent.ConsumeError -> dispatch(NotificationPreferenceReducerEvent.ErrorConsumed)
            }
        }

        override fun reduce(
            state: NotificationPreferenceUiState,
            event: NotificationPreferenceReducerEvent,
        ): NotificationPreferenceUiState =
            when (event) {
                NotificationPreferenceReducerEvent.Loading -> state.copy(isLoading = true, loadFailed = false)
                is NotificationPreferenceReducerEvent.Loaded -> state.copy(isLoading = false, form = event.preferences)
                NotificationPreferenceReducerEvent.LoadFailed -> state.copy(isLoading = false, loadFailed = true)
                is NotificationPreferenceReducerEvent.Edited -> state.copy(form = event.form)
                NotificationPreferenceReducerEvent.Saving -> state.copy(isSaving = true, errorMessage = null)
                is NotificationPreferenceReducerEvent.Saved -> state.copy(isSaving = false, isSaved = true, form = event.preferences)
                is NotificationPreferenceReducerEvent.Failed -> state.copy(isSaving = false, errorMessage = event.message)
                NotificationPreferenceReducerEvent.SavedConsumed -> state.copy(isSaved = false)
                NotificationPreferenceReducerEvent.ErrorConsumed -> state.copy(errorMessage = null)
            }

        private fun load() {
            dispatch(NotificationPreferenceReducerEvent.Loading)
            viewModelScope.launch {
                repository
                    .getPreferences()
                    .onSuccess { dispatch(NotificationPreferenceReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(NotificationPreferenceReducerEvent.LoadFailed) }
            }
        }

        private fun edit(transform: (NotificationPreferences) -> NotificationPreferences) {
            val form = currentState.form ?: return
            if (currentState.isSaving) return
            dispatch(NotificationPreferenceReducerEvent.Edited(transform(form)))
        }

        private fun save() {
            val form = currentState.form ?: return
            if (currentState.isSaving) return
            // 알림을 받는 동안 요일이 하나도 없으면 서버가 거부한다. 요청 전에 막아 이유를 알려 준다.
            if (form.enabled && form.weekdays.isEmpty()) {
                dispatch(
                    NotificationPreferenceReducerEvent.Failed(UiText.Resource(R.string.setting_notification_preference_weekday_required)),
                )
                return
            }
            dispatch(NotificationPreferenceReducerEvent.Saving)
            viewModelScope.launch {
                repository
                    .updatePreferences(form)
                    .onSuccess { dispatch(NotificationPreferenceReducerEvent.Saved(it)) }
                    .onFailure {
                        dispatch(
                            NotificationPreferenceReducerEvent.Failed(
                                UiText.Resource(R.string.setting_notification_preference_save_failed),
                            ),
                        )
                    }
            }
        }

        private companion object {
            const val MIN_GOAL = 1
            const val MAX_GOAL = 7
        }
    }

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value
