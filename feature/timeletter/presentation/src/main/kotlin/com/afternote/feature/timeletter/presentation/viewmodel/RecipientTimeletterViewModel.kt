package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.timeletter.domain.model.ReceivedTimeLetterList
import com.afternote.feature.timeletter.domain.repository.ReceiverTimeLetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecipientTimeletterUiState {
    data object Loading : RecipientTimeletterUiState

    data class Success(
        val letters: ReceivedTimeLetterList,
    ) : RecipientTimeletterUiState

    data object Error : RecipientTimeletterUiState
}

@HiltViewModel
class RecipientTimeletterViewModel
    @Inject
    constructor(
        private val receiverTimeLetterRepository: ReceiverTimeLetterRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<RecipientTimeletterUiState>(RecipientTimeletterUiState.Loading)
        val uiState: StateFlow<RecipientTimeletterUiState> = _uiState.asStateFlow()

        /** 진행 중인 조회 — 첫 진입 이후의 ON_RESUME 이 실행 중인 로드와 겹치면 건너뛰기 위한 가드. */
        private var loadJob: Job? = null

        /**
         * 다음 [refreshOnReturn] 이 첫 ON_RESUME(진입 자체)인지. 첫 resume 은 init 로드와 같은
         * 진입이므로 갱신하지 않는다 — VM 필드인 이유는 ReceiverHomeViewModel 의 refreshOnReturn 과
         * 동일, 프로세스 사망 후 복원에서도 init 로드와 수명이 일치한다.
         */
        private var isFirstResume = true

        init {
            load()
        }

        /** 다른 화면에서 복귀했을 때의 자동 갱신 (#701). 첫 진입은 건너뛰고, 로드가 겹치면 건너뛴다. */
        fun refreshOnReturn() {
            if (isFirstResume) {
                isFirstResume = false
                return
            }
            if (loadJob?.isActive == true) return
            load()
        }

        fun load() {
            loadJob =
                viewModelScope.launch {
                    _uiState.value = RecipientTimeletterUiState.Loading
                    runCatching { receiverTimeLetterRepository.getReceivedTimeLetters() }
                        .onSuccess { letters ->
                            _uiState.value = RecipientTimeletterUiState.Success(letters = letters)
                        }.onFailure {
                            _uiState.value = RecipientTimeletterUiState.Error
                        }
                }
        }
    }
