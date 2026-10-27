package com.afternote.feature.home.presentation.notification

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.notification.NotificationRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 홈 탑바 알림 아이콘의 미읽음 점. 홈이 보일 때마다 다시 센다. */
@HiltViewModel
internal class NotificationBadgeViewModel
    @Inject
    constructor(
        private val repository: NotificationRepository,
    ) : MviViewModel<NotificationBadgeIntent, NotificationBadgeUiState, NotificationBadgeReducerEvent>(NotificationBadgeUiState()) {
        private var countJob: Job? = null

        override fun onIntent(intent: NotificationBadgeIntent) {
            when (intent) {
                NotificationBadgeIntent.Refresh -> count()
            }
        }

        override fun reduce(
            state: NotificationBadgeUiState,
            event: NotificationBadgeReducerEvent,
        ): NotificationBadgeUiState =
            when (event) {
                is NotificationBadgeReducerEvent.Counted -> state.copy(hasUnread = event.unreadCount > 0)
            }

        private fun count() {
            if (countJob?.isActive == true) return
            countJob =
                viewModelScope.launch {
                    // 실패하면 직전 점을 그대로 둔다. 장식이라 실패를 알릴 자리가 없다.
                    repository.inbox().onSuccess { dispatch(NotificationBadgeReducerEvent.Counted(it.unreadCount)) }
                }
        }
    }
