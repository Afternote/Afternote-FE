package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TimeLetterDetailUiState {
    data object Loading : TimeLetterDetailUiState

    data class Success(
        val letter: TimeLetter,
        val receiverNameMap: Map<Long, String>,
    ) : TimeLetterDetailUiState

    data object Error : TimeLetterDetailUiState
}

@HiltViewModel
class TimeLetterDetailViewModel
    @Inject
    constructor(
        private val timeLetterRepository: TimeLetterRepository,
        private val userRepository: UserRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val timeLetterId: Long =
            savedStateHandle.toRoute<TimeLetterRoute.TimeLetterDetailRoute>().timeLetterId

        private val _uiState = MutableStateFlow<TimeLetterDetailUiState>(TimeLetterDetailUiState.Loading)
        val uiState: StateFlow<TimeLetterDetailUiState> = _uiState.asStateFlow()

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
                    _uiState.value = TimeLetterDetailUiState.Loading
                    val receiversDeferred = async { runCatching { userRepository.getReceivers() } }
                    val letterResult = runCatching { timeLetterRepository.getTimeLetter(timeLetterId) }
                    val receivers = receiversDeferred.await().getOrElse { emptyList() }

                    letterResult
                        .onSuccess { letter ->
                            _uiState.value =
                                TimeLetterDetailUiState.Success(
                                    letter = letter,
                                    receiverNameMap = receivers.associate { it.receiverId to it.name },
                                )
                        }.onFailure {
                            _uiState.value = TimeLetterDetailUiState.Error
                        }
                }
        }
    }
