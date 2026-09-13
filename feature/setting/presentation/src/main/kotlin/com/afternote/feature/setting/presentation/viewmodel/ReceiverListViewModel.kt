package com.afternote.feature.setting.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ReceiverListViewModel
    @Inject
    constructor(
        private val userRepository: UserReceiverRepository,
    ) : MviViewModel<ReceiverListIntent, ReceiverListUiState, ReceiverListReducerEvent>(ReceiverListUiState()) {
        private val receivers = userRepository.receiverListFlow
        private var observationJob: Job? = null
        private var stopJob: Job? = null

        override fun onIntent(intent: ReceiverListIntent) {
            when (intent) {
                ReceiverListIntent.ObservationStarted -> startObservation()
                ReceiverListIntent.ObservationStopped -> stopObservation()
            }
        }

        override fun reduce(
            state: ReceiverListUiState,
            event: ReceiverListReducerEvent,
        ): ReceiverListUiState =
            when (event) {
                is ReceiverListReducerEvent.ReceiversChanged -> state.copy(receivers = event.receivers)
            }

        private fun startObservation() {
            val isReturning = stopJob != null
            stopJob?.cancel()
            stopJob = null
            if (observationJob?.isActive == true) {
                // 구독 중단 유예 안에 복귀해도 서버 목록은 갱신한다. 같은 구독의 실패 시 캐시는 보존한다.
                if (isReturning) userRepository.refreshReceiverList()
                return
            }
            observationJob =
                viewModelScope.launch {
                    receivers.collect { receivers ->
                        dispatch(
                            ReceiverListReducerEvent.ReceiversChanged(
                                receivers.map { ReceiverListItem(it.receiverId, it.name, it.relation) },
                            ),
                        )
                    }
                }
        }

        private fun stopObservation() {
            if (stopJob?.isActive == true) return
            stopJob =
                viewModelScope.launch {
                    // 기존 WhileSubscribed(5_000)의 짧은 화면 중단 유예와 복귀 재조회를 유지한다.
                    delay(OBSERVATION_STOP_TIMEOUT_MILLIS)
                    observationJob?.cancel()
                }
        }
    }

private const val OBSERVATION_STOP_TIMEOUT_MILLIS = 5_000L
