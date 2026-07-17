package com.afternote.afternote_fe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.ui.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    ) : ViewModel() {
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
            // ping 실패가 스플래시/네비게이션을 막지 않도록 best-effort(runCatching) 처리.
            viewModelScope.launch {
                authRepository.isLoggedIn
                    .filter { it }
                    .take(1)
                    .collect {
                        runCatching { userRepository.logActivity() }
                    }
            }
        }
    }
