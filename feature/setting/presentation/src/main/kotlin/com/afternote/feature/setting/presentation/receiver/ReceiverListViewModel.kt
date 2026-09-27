package com.afternote.feature.setting.presentation.receiver

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
        private val receiverRepository: UserReceiverRepository,
    ) : MviViewModel<ReceiverListIntent, ReceiverListUiState, ReceiverListReducerEvent>(ReceiverListUiState()) {
        private var observation: Job? = null
        private var pendingStop: Job? = null

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
            pendingStop?.cancel()
            pendingStop = null
            if (observation?.isActive == true) return
            observation =
                viewModelScope.launch {
                    receiverRepository.receiverListFlow.collect { receivers ->
                        dispatch(
                            ReceiverListReducerEvent.ReceiversChanged(
                                receivers.map { ReceiverListItem(it.receiverId, it.name, it.relation) },
                            ),
                        )
                    }
                }
        }

        private fun stopObservation() {
            if (pendingStop?.isActive == true) return
            pendingStop =
                viewModelScope.launch {
                    // 기존 WhileSubscribed(5_000)의 재구독 유예와 마지막 목록을 유지한다.
                    delay(OBSERVATION_STOP_TIMEOUT_MILLIS)
                    observation?.cancel()
                    observation = null
                }
        }

        private companion object {
            const val OBSERVATION_STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
