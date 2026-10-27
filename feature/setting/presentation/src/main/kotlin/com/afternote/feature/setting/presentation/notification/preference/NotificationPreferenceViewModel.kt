package com.afternote.feature.setting.presentation.notification.preference

import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.setting.domain.NotificationPreferenceRepository
import com.afternote.feature.setting.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 푸시 알림 화면의 「알림 시간 설정」 (#2321, Figma 설정 - 푸시알림 설정).
 *
 * 시·분을 고르는 즉시 저장한다 — 시안에 저장 버튼이 없다. 서버 계약은 요일·주간 목표·켜짐도 함께 받으므로
 * 불러온 값을 그대로 실어 시각만 바꾼다. 저장이 실패하면 고르기 전 시각으로 되돌리고 알린다.
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
                is NotificationPreferenceIntent.ChangeTime -> changeTime(intent)
                NotificationPreferenceIntent.ConsumeError -> dispatch(NotificationPreferenceReducerEvent.ErrorConsumed)
            }
        }

        override fun reduce(
            state: NotificationPreferenceUiState,
            event: NotificationPreferenceReducerEvent,
        ): NotificationPreferenceUiState =
            when (event) {
                NotificationPreferenceReducerEvent.Loading -> {
                    state.copy(isLoading = true, loadFailed = false)
                }

                is NotificationPreferenceReducerEvent.Loaded -> {
                    state.copy(isLoading = false, preferences = event.preferences)
                }

                NotificationPreferenceReducerEvent.LoadFailed -> {
                    state.copy(isLoading = false, loadFailed = true)
                }

                is NotificationPreferenceReducerEvent.Saving -> {
                    state.copy(isSaving = true, preferences = event.preferences, errorMessage = null)
                }

                is NotificationPreferenceReducerEvent.Saved -> {
                    state.copy(isSaving = false, preferences = event.preferences)
                }

                is NotificationPreferenceReducerEvent.Failed -> {
                    state.copy(isSaving = false, preferences = event.previous, errorMessage = event.message)
                }

                NotificationPreferenceReducerEvent.ErrorConsumed -> {
                    state.copy(errorMessage = null)
                }
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

        private fun changeTime(intent: NotificationPreferenceIntent.ChangeTime) {
            val previous = currentState.preferences ?: return
            if (currentState.isSaving) return
            val time = intent.time.withSecond(0).withNano(0)
            if (time == previous.time) return
            val requested = previous.copy(time = time)
            dispatch(NotificationPreferenceReducerEvent.Saving(requested))
            viewModelScope.launch {
                repository
                    .updatePreferences(requested)
                    .onSuccess { dispatch(NotificationPreferenceReducerEvent.Saved(it)) }
                    .onFailure {
                        val message = UiText.Resource(R.string.setting_notification_preference_save_failed)
                        dispatch(NotificationPreferenceReducerEvent.Failed(previous, message))
                    }
            }
        }
    }
