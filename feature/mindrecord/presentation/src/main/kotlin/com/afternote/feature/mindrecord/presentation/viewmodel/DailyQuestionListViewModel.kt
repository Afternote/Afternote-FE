package com.afternote.feature.mindrecord.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyQuestionListViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val repository: DailyQuestionRepository,
    ) : ViewModel() {
        private val internalState = MutableStateFlow(InternalState())

        val uiState: StateFlow<DailyQuestionListUiState> =
            internalState
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DailyQuestionListUiState.Loading,
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

                val today = repository.getToday().getOrNull()
                val listResult = repository.getList(date = date)
                val list = listResult.getOrNull().orEmpty()

                if (today == null && listResult.isFailure) {
                    val message =
                        listResult.exceptionOrNull()?.message
                            ?: context.getString(R.string.mindrecord_error_daily_question_list_failed)
                    internalState.update { it.copy(loadPhase = LoadPhase.Failed(message)) }
                } else {
                    internalState.update { it.copy(loadPhase = LoadPhase.Loaded(today, list)) }
                }
            }
        }

        private data class InternalState(
            val loadPhase: LoadPhase = LoadPhase.Loading,
        )

        private sealed interface LoadPhase {
            data object Loading : LoadPhase

            data class Loaded(
                val today: TodayDailyQuestion?,
                val answers: List<DailyQuestion>,
            ) : LoadPhase

            data class Failed(
                val message: String,
            ) : LoadPhase
        }

        private fun InternalState.toUiState(): DailyQuestionListUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> {
                    DailyQuestionListUiState.Loading
                }

                is LoadPhase.Loaded -> {
                    DailyQuestionListUiState.Success(
                        todayQuestion =
                            phase.today?.let {
                                TodayQuestionUi(
                                    questionId = it.questionId,
                                    content = it.content,
                                    isAnswered = it.isAnswered,
                                )
                            },
                        answers = phase.answers.map { it.toUi() },
                    )
                }

                is LoadPhase.Failed -> {
                    DailyQuestionListUiState.Error(phase.message)
                }
            }
    }
