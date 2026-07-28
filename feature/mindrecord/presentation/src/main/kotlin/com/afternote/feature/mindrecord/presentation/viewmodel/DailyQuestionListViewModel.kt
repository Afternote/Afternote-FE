package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
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
import javax.inject.Inject

@HiltViewModel
class DailyQuestionListViewModel
    @Inject
    constructor(
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

        fun delete(id: Long) {
            viewModelScope.launch {
                repository.delete(id).onSuccess { load() }
            }
        }

        private fun load(date: String? = null) {
            viewModelScope.launch {
                internalState.update { it.copy(loadPhase = LoadPhase.Loading) }

                val today = repository.getToday().getOrNull()
                val listResult = repository.getList(date = date)
                // 서버는 draftOnly 없이 조회하면 임시저장을 제외해 내려주지만, 파라미터를 무시하는
                // 서버를 만나도 임시저장이 답변 목록에 새지 않도록 한 겹 더 거른다.
                val list = listResult.getOrNull().orEmpty().filter { !it.isDraft }

                // 목록 조회 실패는 today 성공 여부와 무관하게 드러내야 한다. AND 로 묶으면
                // today 만 성공했을 때 "답변 0개" 로 보여 실패가 화면에서 사라진다.
                if (listResult.isFailure) {
                    val message =
                        UiText.DynamicOrResource(
                            value = listResult.exceptionOrNull()?.message,
                            fallbackResId = R.string.mindrecord_error_daily_question_list_failed,
                        )
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
                val message: UiText,
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
