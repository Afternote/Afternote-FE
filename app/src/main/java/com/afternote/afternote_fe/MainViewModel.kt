package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.core.domain.repository.PendingReceiverInvitationStore
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.ui.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        private val pendingInvitationStore: PendingReceiverInvitationStore,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val pendingNotificationEntryState = MutableStateFlow<NotificationEntryRequest?>(null)

        /** 아직 후속 목적지 adapter가 소비하지 않은 최신 알림 진입 이벤트. */
        internal val pendingNotificationEntry: StateFlow<NotificationEntryRequest?> =
            pendingNotificationEntryState.asStateFlow()

        private val isLoggedIn: StateFlow<Boolean?> =
            authRepository.isLoggedIn.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

        /**
         * 초기 진입 시 null(로딩)이며, [AuthRepository.isLoggedIn]이 방출된 뒤 목적지가 확정된다.
         * null 여부가 기존 `isLoading`과 동일한 역할을 한다.
         */
        val startRoute: StateFlow<Route?> =
            isLoggedIn
                .map { loggedIn ->
                    when (loggedIn) {
                        null -> null
                        true -> Route.Home
                        false -> Route.Onboarding
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = null,
                )

        /**
         * 이 Activity 안에서 처분(수락·보류·거절)이 끝난 초대 토큰 — 다시 띄우지 않는다 (#944).
         * SavedState 에 두어 재생성 뒤에도 같은 토큰으로 랜딩이 또 뜨지 않는다.
         */
        private val settledInvitationToken: StateFlow<String?> =
            savedStateHandle.getStateFlow(SETTLED_INVITATION_TOKEN_KEY, null)

        /** 로그인 뒤 다시 띄우기로 한 토큰 — 랜딩에서 «수락» 을 눌렀는데 로그인 전이었던 경우. */
        private val invitationAwaitingLogin: StateFlow<String?> =
            savedStateHandle.getStateFlow(INVITATION_AWAITING_LOGIN_KEY, null)

        /**
         * 지금 랜딩을 띄워야 하는 초대 토큰. null 이면 띄울 것이 없다.
         *
         * 보관 토큰이 있고, 이 Activity 에서 아직 처분되지 않았으며, 로그인 대기로 미뤄 둔 토큰이면
         * 로그인이 확정된 뒤에만 값을 낸다. 로그인 여부는 [startRoute] 와 같은 스트림을 본다 —
         * 앱 루트의 인증 경계를 그대로 재사용한다.
         */
        val pendingInvitationToken: StateFlow<String?> =
            combine(
                pendingInvitationStore.pendingToken,
                isLoggedIn,
                settledInvitationToken,
                invitationAwaitingLogin,
            ) { token, loggedIn, settled, awaitingLogin ->
                when {
                    token == null || loggedIn == null -> null
                    token == settled -> null
                    token == awaitingLogin && !loggedIn -> null
                    else -> token
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

        /** 스킴 Intent 에서 뽑은 토큰을 보관한다. 같은 Intent 가 재생성으로 다시 와도 같은 값이라 무해하다. */
        internal fun enqueueInvitationToken(token: String) {
            viewModelScope.launch { pendingInvitationStore.save(token) }
        }

        /** 랜딩에서 로그인이 필요하다고 판정됐다 — 로그인이 끝날 때까지 이 토큰의 랜딩을 미룬다. */
        internal fun deferInvitationUntilLogin(token: String) {
            savedStateHandle[INVITATION_AWAITING_LOGIN_KEY] = token
        }

        /** 랜딩이 닫혔다(수락 완료·나중에·거절 안내 확인). 이 Activity 에서 이 토큰으로는 다시 띄우지 않는다. */
        internal fun settleInvitation(token: String) {
            savedStateHandle[SETTLED_INVITATION_TOKEN_KEY] = token
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
            const val SETTLED_INVITATION_TOKEN_KEY = "settled_receiver_invitation_token"
            const val INVITATION_AWAITING_LOGIN_KEY = "receiver_invitation_awaiting_login"
        }
    }
