package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.data.cache.ReceiverCacheStore
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
        private val receiverCacheStore: ReceiverCacheStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<TimeletterUiState>(TimeletterUiState.Loading)
        val uiState: StateFlow<TimeletterUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.value = TimeletterUiState.Loading
                val receiversDeferred = async { receiverCacheStore.ensureLoaded() }
                val lettersResult = runCatching { timeLetterRepository.getTimeLetters() }
                receiversDeferred.await()

                lettersResult
                    .onSuccess { letters ->
                        _uiState.value = TimeletterUiState.Success(
                            letters = letters,
                            receiverNameMap = receiverCacheStore.receiverNameMap.value,
                        )
                    }.onFailure {
                        _uiState.value = TimeletterUiState.Error("타임레터를 불러올 수 없습니다.")
                    }
            }
        }
    }
