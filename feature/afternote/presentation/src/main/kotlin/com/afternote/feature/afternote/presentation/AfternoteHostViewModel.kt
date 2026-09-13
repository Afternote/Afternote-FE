package com.afternote.feature.afternote.presentation

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Afternote 그래프의 프로필 상태. 지문 인증 화면을 떠난 뒤에도 기존 WhileSubscribed와 같은 5초 유예를 둔다. */
@HiltViewModel
internal class AfternoteHostViewModel
    @Inject
    constructor(
        private val userProfileRepository: UserProfileCacheRepository,
    ) : MviViewModel<AfternoteHostIntent, AfternoteHostUiState, AfternoteHostReducerEvent>(AfternoteHostUiState()) {
        private var profileJob: Job? = null
        private var stopJob: Job? = null

        override fun onIntent(intent: AfternoteHostIntent) {
            when (intent) {
                AfternoteHostIntent.ObserveProfile -> {
                    stopJob?.cancel()
                    if (profileJob?.isActive == true) return
                    profileJob =
                        viewModelScope.launch {
                            userProfileRepository.isPasskeyRegisteredFlow().collect {
                                dispatch(AfternoteHostReducerEvent.PasskeyRegistrationChanged(it))
                            }
                        }
                }

                AfternoteHostIntent.StopObservingProfile -> {
                    stopJob?.cancel()
                    stopJob =
                        viewModelScope.launch {
                            delay(5_000)
                            profileJob?.cancel()
                        }
                }
            }
        }

        override fun reduce(
            state: AfternoteHostUiState,
            event: AfternoteHostReducerEvent,
        ): AfternoteHostUiState =
            when (event) {
                is AfternoteHostReducerEvent.PasskeyRegistrationChanged -> state.copy(isPasskeyRegistered = event.registered)
            }
    }
