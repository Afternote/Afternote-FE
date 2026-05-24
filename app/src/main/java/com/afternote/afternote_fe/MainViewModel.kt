package com.afternote.afternote_fe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.auth.AuthRepository
import com.afternote.core.ui.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
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
    }
