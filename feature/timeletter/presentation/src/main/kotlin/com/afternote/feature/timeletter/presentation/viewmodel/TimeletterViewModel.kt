package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimeletterViewModel
    @Inject
    constructor(
        private val timeLetterRepository: TimeLetterRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<TimeletterUiState>(TimeletterUiState.Loading)
        val uiState: StateFlow<TimeletterUiState> = _uiState.asStateFlow()

        private var allLetters: TimeLetterList? = null
        private var selectedFilterReceiverIds: Set<Long> = emptySet()
        private var receiverNameMap: Map<Long, String> = emptyMap()

        fun setReceiverFilter(receiverIds: List<Long>) {
            selectedFilterReceiverIds = receiverIds.toSet()
            applyFilter()
        }

        private fun applyFilter() {
            val letters = allLetters ?: return
            val filterIds = selectedFilterReceiverIds
            val currentState = _uiState.value as? TimeletterUiState.Success
            val filteredLetters =
                if (filterIds.isEmpty()) {
                    letters
                } else {
                    val filtered =
                        letters.timeLetters.filter { letter ->
                            letter.receiverIds.any { it in filterIds }
                        }
                    letters.copy(timeLetters = filtered, totalCount = filtered.size)
                }
            _uiState.value =
                TimeletterUiState.Success(
                    letters = filteredLetters,
                    receiverNameMap = receiverNameMap,
                    selectedFilterReceiverIds = filterIds,
                    isDeleting = currentState?.isDeleting ?: false,
                    showDeleteFailure = currentState?.showDeleteFailure ?: false,
                    errorMessage = currentState?.errorMessage,
                )
        }

        fun load() {
            viewModelScope.launch {
                _uiState.value = TimeletterUiState.Loading
                val receiversDeferred = async { runCatching { userRepository.getReceivers() } }
                val lettersResult = runCatching { timeLetterRepository.getTimeLetters() }

                receiverNameMap =
                    receiversDeferred
                        .await()
                        .getOrElse { emptyList() }
                        .associate { it.receiverId to it.name }

                lettersResult
                    .onSuccess { letters ->
                        allLetters = letters
                        applyFilter()
                    }.onFailure {
                        _uiState.value = TimeletterUiState.Error("타임레터를 불러올 수 없습니다.")
                    }
            }
        }

        fun deleteTimeLetter(timeLetterId: Long) {
            val currentState = _uiState.value as? TimeletterUiState.Success ?: return
            if (currentState.isDeleting) return
            _uiState.value =
                currentState.copy(
                    isDeleting = true,
                    showDeleteFailure = false,
                    errorMessage = null,
                )

            viewModelScope.launch {
                runCatchingCancellable { timeLetterRepository.deleteTimeLetters(listOf(timeLetterId)) }
                    .onSuccess { load() }
                    .onFailure {
                        val latestState = _uiState.value
                        if (latestState is TimeletterUiState.Success) {
                            _uiState.value =
                                latestState.copy(
                                    isDeleting = false,
                                    showDeleteFailure = true,
                                )
                        }
                    }
            }
        }

        fun consumeErrorMessage() {
            val currentState = _uiState.value
            if (currentState is TimeletterUiState.Success) {
                _uiState.value = currentState.copy(errorMessage = null)
            }
        }

        fun consumeDeleteFailure() {
            val currentState = _uiState.value
            if (currentState is TimeletterUiState.Success) {
                _uiState.value = currentState.copy(showDeleteFailure = false)
            }
        }
    }
