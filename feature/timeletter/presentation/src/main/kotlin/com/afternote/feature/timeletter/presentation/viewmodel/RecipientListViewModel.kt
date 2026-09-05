package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.model.setting.ReceiverListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface RecipientListUiState {
    data object Loading : RecipientListUiState

    data class Success(
        val recipients: List<ReceiverListItem>,
    ) : RecipientListUiState

    data object Error : RecipientListUiState
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RecipientListViewModel
    @Inject
    constructor(
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val retryRevision = MutableStateFlow(0L)

        val uiState: StateFlow<RecipientListUiState> =
            retryRevision
                .flatMapLatest {
                    flow {
                        emit(RecipientListUiState.Loading)
                        // receiverListFlow는 실패를 예외로 흘리지 않고 마지막 성공 목록 또는 빈
                        // 목록으로 낮춘다(#1099) — 이 화면이 실패를 그 정책과 독립적으로 보려면
                        // 별도 suspend 조회로 확인해야 한다.
                        val probe = runCatchingCancellable { userRepository.getReceivers() }
                        if (probe.isFailure) {
                            emit(RecipientListUiState.Error)
                        } else {
                            emitAll(
                                userRepository.receiverListFlow.map { receivers ->
                                    RecipientListUiState.Success(
                                        receivers.map { ReceiverListItem(it.receiverId, it.name, it.relation) },
                                    )
                                },
                            )
                        }
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = RecipientListUiState.Loading,
                )

        fun retry() {
            retryRevision.update { it + 1 }
        }
    }
