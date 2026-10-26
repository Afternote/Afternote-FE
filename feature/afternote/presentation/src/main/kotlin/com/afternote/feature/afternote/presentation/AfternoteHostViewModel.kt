package com.afternote.feature.afternote.presentation

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserProfileCacheRepository
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [com.afternote.core.ui.Route.Afternote] 엔트리 스코프의 상태 정본.
 * [com.afternote.feature.afternote.presentation.navigation.AfternoteNavHost] 가 자기 스코프에서 만들어
 * 로컬 스택의 화면들이 같은 인스턴스를 본다.
 *
 * 에디터 flow 상태는 flow-scoped
 * [com.afternote.feature.afternote.presentation.editor.AfternoteEditorViewModel]이 담당한다.
 * 본 ViewModel은 애프터노트 로컬 스택 전체에서 공유하는 사용자 상태만 보유한다.
 * 프로필 구독은 마지막 화면이 떠난 뒤 기존 WhileSubscribed와 같은 5초 유예를 둔다.
 */
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
