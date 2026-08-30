package com.afternote.afternote_fe.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `POST_NOTIFICATIONS` 권한을 언제 물을지 정한다 (#1454).
 *
 * 물어야 할 시점은 **로그인이 확정된 뒤**다. 알림(일일 리마인더·FCM)의 수혜자가 로그인 사용자이고,
 * 온보딩 첫 화면에서 맥락 없이 다이얼로그를 띄우면 거부만 유도하기 때문이다. 요청은 기기당 1회이며
 * 허용·거부 어느 쪽이든 기록해 다시 묻지 않는다 — 거부한 사용자의 복구 경로는 설정 > 푸시 알림의
 * 기존 «기기 알림 설정» 행이다(새 UX 를 만들지 않는다).
 */
@HiltViewModel
class NotificationPermissionViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        private val store: NotificationPermissionRequestStore,
    ) : ViewModel() {
        val shouldRequest: StateFlow<Boolean> =
            combine(
                authRepository.isLoggedIn,
                store.hasRequested,
            ) { isLoggedIn, hasRequested ->
                isLoggedIn && !hasRequested
            }.distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = false,
                )

        /**
         * 다이얼로그를 띄운 사실을 기록한다. 기록 실패(디스크 오류)는 삼킨다 — 다음 실행에서 한 번 더
         * 묻게 될 뿐이고, 시스템이 두 번 거부한 뒤에는 다이얼로그 자체를 띄우지 않는다.
         */
        fun markRequested() {
            viewModelScope.launch {
                runCatchingCancellable { store.markRequested() }
            }
        }
    }
