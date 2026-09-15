package com.afternote.afternote_fe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.afternote_fe.notification.NotificationEntryRequest
import com.afternote.core.common.deeplink.AfternoteAppLinkParser
import com.afternote.core.common.deeplink.AppLinkResolution
import com.afternote.core.common.deeplink.AuthGate
import com.afternote.core.common.deeplink.NavigationTarget
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.ui.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val pendingNotificationEntryState = MutableStateFlow<NotificationEntryRequest?>(null)

        /** 아직 후속 목적지 adapter가 소비하지 않은 최신 알림 진입 이벤트. */
        internal val pendingNotificationEntry: StateFlow<NotificationEntryRequest?> =
            pendingNotificationEntryState.asStateFlow()

        /**
         * 아직 재개하지 않은 App Link 목적지.
         *
         * 초기값을 [SavedStateHandle]에서 되읽는 것이 프로세스 재생성 대비의 전부다 — Activity가
         * 다시 살아날 때 셸은 이 값만 보면 되고, 같은 Intent를 다시 해석하지 않는다.
         */
        private val pendingAppLinkTargetState =
            MutableStateFlow(
                savedStateHandle.get<String>(PENDING_APP_LINK_TARGET_KEY)?.let(::restoredAppLinkTarget),
            )

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
         * 관문을 통과해 **지금 재개할 수 있는** 링크 목적지. 통과하지 못한 관문이 남아 있으면
         * null 이라, 소비처는 갈 수 있을 때에만 값을 본다.
         *
         * 관문은 두 층이 나눠 지킨다. 로그인은 앱 셸의 관문이라 여기서 판정하고, 지문은 목적지
         * 그래프 자신이 지킨다([Route.Afternote]의 시작 화면이 지문 관문이다) — 그래서 여기서
         * 보는 것은 [AuthGate.LOGIN] 하나다. 어떤 관문이 필요한지는 하드코딩하지 않고 목적지가
         * 싣고 온 [NavigationTarget.requiredGates]를 읽는다.
         */
        internal val resumableAppLinkTarget: StateFlow<NavigationTarget?> =
            combine(
                pendingAppLinkTargetState,
                authRepository.isLoggedIn,
            ) { target, isLoggedIn ->
                target?.takeIf { AuthGate.LOGIN !in it.requiredGates || isLoggedIn }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

        init {
            observeLogoutForAppLinkDiscard()
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

        /**
         * 링크가 준 목적지를 큐에 넣는다. 뒤에 온 링크가 이긴다 — 사용자가 방금 누른 링크가
         * 아직 관문에 걸려 있던 옛 목적지보다 최신 의도다.
         */
        internal fun enqueueAppLinkTarget(target: NavigationTarget) {
            savedStateHandle[PENDING_APP_LINK_TARGET_KEY] = AfternoteAppLinkParser.canonicalUrl(target)
            pendingAppLinkTargetState.value = target
        }

        /**
         * 재개를 마친 목적지를 비운다. 재개 도중 새 링크가 도착해 pending 이 바뀌었으면 그 새
         * 목적지를 지우지 않는다.
         */
        internal fun consumeAppLinkTarget(target: NavigationTarget) {
            if (pendingAppLinkTargetState.value != target) return
            discardAppLinkTarget()
        }

        private fun discardAppLinkTarget() {
            savedStateHandle[PENDING_APP_LINK_TARGET_KEY] = null
            pendingAppLinkTargetState.value = null
        }

        /**
         * 로그아웃하면 기다리던 목적지를 버린다.
         *
         * 남겨 두면 다음 로그인이 링크가 가리키던 화면을 연다 — 그 사이 계정이 바뀌었을 수 있어
         * 남의 자료를 여는 경로가 된다. 로그인 전부터 기다리던 목적지(앱이 잠겨 있을 때 누른
         * 링크)는 살려야 하므로, 「false 로 시작」이 아니라 **true 였다가 false 가 된 전이**만 본다.
         */
        private fun observeLogoutForAppLinkDiscard() {
            viewModelScope.launch {
                var wasLoggedIn = false
                authRepository.isLoggedIn.distinctUntilChanged().collect { isLoggedIn ->
                    if (wasLoggedIn && !isLoggedIn) discardAppLinkTarget()
                    wasLoggedIn = isLoggedIn
                }
            }
        }

        private companion object {
            const val CONSUMED_NOTIFICATION_IDENTITY_KEY = "consumed_notification_entry_identity"
            const val PENDING_APP_LINK_TARGET_KEY = "pending_app_link_target_url"
        }
    }

/**
 * 저장해 둔 목적지를 되읽는다. 저장 형식이 정규 URL 이라 되읽기가 곧 계약 왕복이고, 계약에서
 * 빠진 경로가 저장돼 있었다면(옛 버전이 남긴 값) 해석되지 않아 조용히 버려진다.
 */
private fun restoredAppLinkTarget(savedUrl: String): NavigationTarget? =
    (AfternoteAppLinkParser.parse(savedUrl) as? AppLinkResolution.Resolved)?.target
