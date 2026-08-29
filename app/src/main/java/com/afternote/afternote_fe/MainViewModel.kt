package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.ui.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        private val userRepository: UserRepository,
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

        init {
            // 로그인 상태가 처음 true 로 확정되는 시점에 활동 기록 ping 을 1회 전송 (미사용 타이머 리셋, 이슈 #429).
            // filter { it } 는 isLoggedIn(Boolean) 이 true(로그인) 인 방출만 통과시킨다 (= filter { it == true },
            // it 이 이미 Boolean 이라 == true 생략한 관용구). 이어 take(1) 는 그중 첫 1건을 받으면 flow 를
            // 완료(구독 취소)해 이후 true 재방출(토큰 갱신·DataStore 재방출 등)을 무시 → 중복 ping 을 막는다.
            // 결과: 프로세스(= MainViewModel 인스턴스)당 한 번만 발화. 앱 진입 Activity 스코프라 사실상 앱 실행당 1회.
            // WhileSubscribed 재구독으로 인한 중복 방출과 무관하게 최초 로그인 확정 1건만 소비한다.
            // ping 실패가 스플래시/네비게이션을 막지 않도록 best-effort 처리 — 단 취소는 삼키지 않는다.
            viewModelScope.launch {
                authRepository.isLoggedIn
                    .filter { it }
                    .take(1)
                    .collect {
                        runCatchingCancellable { userRepository.logActivity() }
                    }
            }
        }

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
