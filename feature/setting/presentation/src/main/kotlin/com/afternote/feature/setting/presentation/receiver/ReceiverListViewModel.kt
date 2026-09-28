package com.afternote.feature.setting.presentation.receiver

import androidx.lifecycle.viewModelScope
import com.afternote.core.domain.model.ReceiverListState
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.Receiver
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
                ReceiverListIntent.Retry -> retry()
            }
        }

        override fun reduce(
            state: ReceiverListUiState,
            event: ReceiverListReducerEvent,
        ): ReceiverListUiState =
            when (event) {
                ReceiverListReducerEvent.RetryStarted -> state.copy(loadState = ReceiverListLoadState.Loading)
                is ReceiverListReducerEvent.RepositoryStateChanged -> state.withRepositoryState(event.state)
            }

        private fun retry() {
            if (observation?.isActive != true) return
            when (currentState.loadState) {
                ReceiverListLoadState.Failure, ReceiverListLoadState.RefreshFailure -> {
                    dispatch(ReceiverListReducerEvent.RetryStarted)
                    receiverRepository.refreshReceiverList()
                }

                ReceiverListLoadState.Loading, ReceiverListLoadState.Ready -> {
                    Unit
                }
            }
        }

        private fun startObservation() {
            pendingStop?.cancel()
            pendingStop = null
            if (observation?.isActive == true) return
            observation =
                viewModelScope.launch {
                    receiverRepository.receiverListStateFlow.collect { state ->
                        dispatch(ReceiverListReducerEvent.RepositoryStateChanged(state))
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

private fun ReceiverListUiState.withRepositoryState(state: ReceiverListState): ReceiverListUiState =
    when (state) {
        ReceiverListState.SignedOut -> {
            ReceiverListUiState()
        }

        is ReceiverListState.Loading -> {
            copy(
                receivers = state.previousReceivers?.toItems() ?: receivers.takeIf { sessionId == state.sessionId }.orEmpty(),
                loadState = ReceiverListLoadState.Loading,
                sessionId = state.sessionId,
            )
        }

        is ReceiverListState.Success -> {
            copy(
                receivers = state.receivers.toItems(),
                loadState = ReceiverListLoadState.Ready,
            )
        }

        is ReceiverListState.Failure -> {
            val remaining =
                when {
                    state.discardPrevious -> emptyList()
                    else -> state.previousReceivers?.toItems() ?: receivers
                }
            copy(
                receivers = remaining,
                loadState = if (remaining.isEmpty()) ReceiverListLoadState.Failure else ReceiverListLoadState.RefreshFailure,
            )
        }
    }

private fun List<Receiver>.toItems(): List<ReceiverListItem> = map { ReceiverListItem(it.receiverId, it.name, it.relation) }
