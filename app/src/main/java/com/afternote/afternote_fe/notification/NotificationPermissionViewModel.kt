package com.afternote.afternote_fe.notification

import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
internal class NotificationPermissionViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        private val store: NotificationPermissionRequestStore,
    ) : MviViewModel<NotificationPermissionIntent, NotificationPermissionUiState, NotificationPermissionReducerEvent>(
            NotificationPermissionUiState(),
        ) {
        private val requestEligibility =
            combine(
                authRepository.isLoggedIn,
                store.hasRequested,
            ) { isLoggedIn, hasRequested ->
                isLoggedIn && !hasRequested
            }.distinctUntilChanged()

        private var observationJob: Job? = null
        private var stopJob: Job? = null

        override fun onIntent(intent: NotificationPermissionIntent) {
            when (intent) {
                NotificationPermissionIntent.ObservationStarted -> startObservation()
                NotificationPermissionIntent.ObservationStopped -> stopObservation()
                NotificationPermissionIntent.RecordRequest -> markRequested()
            }
        }

        override fun reduce(
            state: NotificationPermissionUiState,
            event: NotificationPermissionReducerEvent,
        ): NotificationPermissionUiState =
            when (event) {
                is NotificationPermissionReducerEvent.EligibilityChanged -> state.copy(shouldRequest = event.shouldRequest)
            }

        private fun startObservation() {
            stopJob?.cancel()
            if (observationJob?.isActive == true) return
            observationJob =
                viewModelScope.launch {
                    requestEligibility.collect { shouldRequest ->
                        dispatch(NotificationPermissionReducerEvent.EligibilityChanged(shouldRequest))
                    }
                }
        }

        private fun stopObservation() {
            if (stopJob?.isActive == true) return
            stopJob =
                viewModelScope.launch {
                    // 기존 WhileSubscribed(5_000)처럼 잠깐의 화면 중단에는 구독을 유지한다.
                    delay(OBSERVATION_STOP_TIMEOUT_MILLIS)
                    observationJob?.cancel()
                }
        }

        /**
         * 다이얼로그를 띄운 사실을 기록한다. 기록 실패(디스크 오류)는 삼킨다 — 다음 실행에서 한 번 더
         * 묻게 될 뿐이고, 시스템이 두 번 거부한 뒤에는 다이얼로그 자체를 띄우지 않는다.
         */
        private fun markRequested() {
            viewModelScope.launch {
                runCatchingCancellable { store.markRequested() }
            }
        }
    }

private const val OBSERVATION_STOP_TIMEOUT_MILLIS = 5_000L
