package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.data.cache.ReceiverCacheStore
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.usecase.GetTimeLettersUseCase
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
        private val getTimeLettersUseCase: GetTimeLettersUseCase,
        private val receiverCacheStore: ReceiverCacheStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<TimeletterUiState>(TimeletterUiState.Loading)
        val uiState: StateFlow<TimeletterUiState> = _uiState.asStateFlow()

        private var allLetters: TimeLetterList? = null
        private var selectedFilterReceiverIds: Set<Long> = emptySet()

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
                    receiverNameMap = receiverCacheStore.receiverNameMap.value,
                    selectedFilterReceiverIds = filterIds,
                )
        }

        fun load() {
            viewModelScope.launch {
                _uiState.value = TimeletterUiState.Loading
                val receiversDeferred = async { receiverCacheStore.ensureLoaded() }
                val lettersResult = runCatching { getTimeLettersUseCase() }
                runCatching { receiversDeferred.await() }

                lettersResult
                    .onSuccess { letters ->
                        allLetters = letters
                        applyFilter()
                    }.onFailure {
                        _uiState.value = TimeletterUiState.Error("타임레터를 불러올 수 없습니다.")
                    }
            }
        }
    }
