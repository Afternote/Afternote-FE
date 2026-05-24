package com.afternote.feature.timeletter.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.afternote.core.data.cache.ReceiverCacheStore
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.usecase.GetTimeLetterUseCase
import com.afternote.feature.timeletter.presentation.navigation.TimeLetterRoute
import dagger.hilt.android.lifecycle.HiltViewModel
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
        private val getTimeLetterUseCase: GetTimeLetterUseCase,
        private val receiverCacheStore: ReceiverCacheStore,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val timeLetterId: Long =
            savedStateHandle.toRoute<TimeLetterRoute.TimeLetterDetailRoute>().timeLetterId

        private val _uiState = MutableStateFlow<TimeLetterDetailUiState>(TimeLetterDetailUiState.Loading)
        val uiState: StateFlow<TimeLetterDetailUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.value = TimeLetterDetailUiState.Loading
                val receiversDeferred = async { receiverCacheStore.ensureLoaded() }
                val letterResult = runCatching { getTimeLetterUseCase(timeLetterId) }
                runCatching { receiversDeferred.await() }

                letterResult
                    .onSuccess { letter ->
                        _uiState.value =
                            TimeLetterDetailUiState.Success(
                                letter = letter,
                                receiverNameMap = receiverCacheStore.receiverNameMap.value,
                            )
                    }.onFailure {
                        _uiState.value = TimeLetterDetailUiState.Error
                    }
            }
        }
    }
