package com.afternote.feature.afternote.presentation.receiver.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.afternote.domain.repository.receiver.ReceiverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신인별 애프터노트 목록 화면 ViewModel.
 *
 * GET /api/v1/receiver-auth/after-notes API로 전달된 애프터노트 목록을 조회합니다.
 * `X-Auth-Code` 헤더는 ReceiverAuthInterceptor가 자동 부착합니다.
 */
@HiltViewModel
class ReceiverAfternotesListViewModel
    @Inject
    constructor(
        private val receiverRepository: ReceiverRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReceiverAfternotesListUiState())
        val uiState: StateFlow<ReceiverAfternotesListUiState> = _uiState.asStateFlow()

        init {
            loadAfterNotes()
        }

        fun onEvent(event: ReceiverAfternotesListEvent) {
            when (event) {
                ReceiverAfternotesListEvent.Load -> loadAfterNotes()
                ReceiverAfternotesListEvent.Retry -> retry()
                ReceiverAfternotesListEvent.ErrorConsumed -> clearError()
            }
        }

        private fun loadAfterNotes() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                receiverRepository
                    .getReceivedAfterNotes()
                    .onSuccess { data ->
                        _uiState.update {
                            it.copy(
                                items =
                                    data.items.map { item ->
                                        ReceivedAfternoteListItemUi(
                                            id = item.id,
                                            title = item.title.orEmpty(),
                                            sourceType = item.sourceType.orEmpty(),
                                            lastUpdatedAt = item.lastUpdatedAt.orEmpty(),
                                        )
                                    },
                                isLoading = false,
                                errorMessage = null,
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "애프터노트 목록 조회에 실패했습니다.",
                            )
                        }
                    }
            }
        }

        private fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun retry() {
            clearError()
            loadAfterNotes()
        }
    }
