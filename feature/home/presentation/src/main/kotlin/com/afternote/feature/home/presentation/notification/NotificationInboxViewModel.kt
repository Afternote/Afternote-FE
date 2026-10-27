package com.afternote.feature.home.presentation.notification

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.notification.NotificationRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 서버 알림함 (#2321). 푸시를 못 받은 기기·하루 상한으로 인앱에만 남은 알림도 여기서 본다.
 *
 * 첫 페이지(최근 50개)만 보여 준다. 읽음 처리는 항목을 눌러 목적지를 확인하는 앱 셸이 서버에 보낸다.
 */
@HiltViewModel
internal class NotificationInboxViewModel
    @Inject
    constructor(
        private val repository: NotificationRepository,
    ) : MviViewModel<NotificationInboxIntent, NotificationInboxUiState, NotificationInboxReducerEvent>(NotificationInboxUiState()) {
        private var loadJob: Job? = null

        init {
            load()
        }

        override fun onIntent(intent: NotificationInboxIntent) {
            when (intent) {
                NotificationInboxIntent.Refresh -> load()
                is NotificationInboxIntent.Open -> dispatch(NotificationInboxReducerEvent.Opened(intent.notificationId))
            }
        }

        override fun reduce(
            state: NotificationInboxUiState,
            event: NotificationInboxReducerEvent,
        ): NotificationInboxUiState =
            when (event) {
                // 이미 그린 목록이 있으면 다시 불러오는 동안에도 그대로 둔다.
                NotificationInboxReducerEvent.Loading -> {
                    state.copy(isLoading = state.items.isEmpty(), loadFailed = false)
                }

                is NotificationInboxReducerEvent.Loaded -> {
                    state.copy(isLoading = false, items = event.items)
                }

                // 보던 목록이 있으면 실패를 덮어쓰지 않는다 — 일시적 실패로 목록이 사라지면 인과가 안 보인다.
                NotificationInboxReducerEvent.LoadFailed -> {
                    state.copy(isLoading = false, loadFailed = state.items.isEmpty())
                }

                is NotificationInboxReducerEvent.Opened -> {
                    state.copy(items = state.items.map { if (it.id == event.notificationId) it.copy(isRead = true) else it })
                }
            }

        private fun load() {
            if (loadJob?.isActive == true) return
            dispatch(NotificationInboxReducerEvent.Loading)
            loadJob =
                viewModelScope.launch {
                    repository
                        .inbox()
                        .onSuccess { dispatch(NotificationInboxReducerEvent.Loaded(it.items)) }
                        .onFailure { dispatch(NotificationInboxReducerEvent.LoadFailed) }
                }
        }
    }
