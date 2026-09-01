package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.ui.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val pendingNotificationEntryState = MutableStateFlow<NotificationEntryRequest?>(null)

        /** 아직 후속 목적지 adapter가 소비하지 않은 최신 알림 진입 이벤트. */
        internal val pendingNotificationEntry: StateFlow<NotificationEntryRequest?> =
            pendingNotificationEntryState.asStateFlow()

        /**
         * 초기 진입 시 null(로딩)이며, [AuthRepository.isLoggedIn]이 방출된 뒤 목적지가 확정된다.
         * null 여부가 기존 `isLoading`과 동일한 역할을 한다.
         */
        val startRoute: StateFlow<Route?> =
            authRepository.isLoggedIn
                .map { isLoggedIn ->
                    if (isLoggedIn) Route.Home else Route.Onboarding
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )

        /**
         * 새 알림 발생을 큐에 넣는다. Activity 재생성 때 같은 initial Intent가 다시 전달되더라도
         * [SavedStateHandle]에 기록한 마지막 소비 identity면 다시 수신 이벤트를 만들지 않는다.
         */
        internal fun enqueueNotificationEntry(request: NotificationEntryRequest) {
            val identityKey = request.identityKey
            if (savedStateHandle.get<String>(CONSUMED_NOTIFICATION_IDENTITY_KEY) == identityKey) return
            if (pendingNotificationEntryState.value?.identityKey == identityKey) return
            pendingNotificationEntryState.value = request
        }

        /**
         * 처리한 identity가 아직 pending인 요청과 같을 때만 비운다. 처리 도중 더 최신 알림이
         * 도착해 pending 값이 바뀌면 이전 처리 완료가 새 요청을 지우지 않는다.
         */
        internal fun consumeNotificationEntry(identityKey: String) {
            if (pendingNotificationEntryState.value?.identityKey != identityKey) return
            savedStateHandle[CONSUMED_NOTIFICATION_IDENTITY_KEY] = identityKey
            pendingNotificationEntryState.value = null
        }

        private companion object {
            const val CONSUMED_NOTIFICATION_IDENTITY_KEY = "consumed_notification_entry_identity"
        }
    }
