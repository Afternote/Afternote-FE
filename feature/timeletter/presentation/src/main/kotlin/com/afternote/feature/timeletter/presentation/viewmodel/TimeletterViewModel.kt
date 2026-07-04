package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            viewModelScope.launch {
                runCatching { timeLetterRepository.deleteTimeLetters(listOf(timeLetterId)) }
                    .onSuccess { load() }
                    .onFailure {
                        _uiState.value = TimeletterUiState.Error("타임레터를 삭제할 수 없습니다.")
                    }
            }
        }
    }
