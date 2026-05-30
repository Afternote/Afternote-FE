package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.Diary
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
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

        fun refresh(yearMonth: YearMonth = YearMonth.now()) {
            load(yearMonth)
        }

        fun delete(id: Long) {
            viewModelScope.launch {
                repository.delete(id).onSuccess { load() }
            }
        }

        private fun load(yearMonth: YearMonth = YearMonth.now()) {
            viewModelScope.launch {
                internalState.update { it.copy(loadPhase = LoadPhase.Loading) }
                repository
                    .getList(yearMonth = yearMonth.toString(), draftOnly = null)
                    .onSuccess { list ->
                        internalState.update { it.copy(loadPhase = LoadPhase.Loaded(list)) }
                    }.onFailure { e ->
                        internalState.update {
                            it.copy(
                                loadPhase =
                                    LoadPhase.Failed(
                                        UiText.DynamicOrResource(
                                            value = e.message,
                                            fallbackResId = R.string.mindrecord_error_diary_list_failed,
                                        ),
                                    ),
                            )
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
                val message: UiText,
            ) : LoadPhase
        }

        private fun InternalState.toUiState(): DiaryListUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> DiaryListUiState.Loading
                is LoadPhase.Loaded -> DiaryListUiState.Success(phase.diaries.map { it.toUi() })
                is LoadPhase.Failed -> DiaryListUiState.Error(phase.message)
            }
    }
