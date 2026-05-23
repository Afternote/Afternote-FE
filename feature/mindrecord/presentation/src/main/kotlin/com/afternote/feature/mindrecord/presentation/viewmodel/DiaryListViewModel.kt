package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiaryListViewModel
    @Inject
    constructor(
        private val repository: DiaryRepository,
    ) : ViewModel() {
        private val internalState = MutableStateFlow(InternalState())

        val uiState: StateFlow<DiaryListUiState> =
            internalState
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DiaryListUiState.Loading,
                )

        init {
            load()
        }

        fun refresh(date: String? = null) {
            load(date)
        }

        private fun load(date: String? = null) {
            viewModelScope.launch {
                internalState.update { it.copy(loadPhase = LoadPhase.Loading) }
                repository
                    .getList(date = date)
                    .onSuccess { list ->
                        internalState.update { it.copy(loadPhase = LoadPhase.Loaded(list)) }
                    }.onFailure { e ->
                        internalState.update {
                            it.copy(loadPhase = LoadPhase.Failed(e.message ?: "일기를 불러오지 못했습니다."))
                        }
                    }
            }
        }

        private data class InternalState(
            val loadPhase: LoadPhase = LoadPhase.Loading,
        )

        private sealed interface LoadPhase {
            data object Loading : LoadPhase

            data class Loaded(
                val diaries: List<Diary>,
            ) : LoadPhase

            data class Failed(
                val message: String,
            ) : LoadPhase
        }

        private fun InternalState.toUiState(): DiaryListUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> DiaryListUiState.Loading
                is LoadPhase.Loaded -> DiaryListUiState.Success(phase.diaries.map { it.toUi() })
                is LoadPhase.Failed -> DiaryListUiState.Error(phase.message)
            }
    }
