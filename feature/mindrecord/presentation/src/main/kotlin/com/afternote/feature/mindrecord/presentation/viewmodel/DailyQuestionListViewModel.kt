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
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
        private var loadJob: Job? = null

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

        /**
         * 탭 전환·작성 화면 복귀 등 사용자가 요청하지 않은 자동 갱신.
         *
         * 화면이 살아 있는 채로 발화하므로 로딩을 방출하지 않고(콘텐츠가 통째 교체되면
         * 캘린더 월과 스크롤 위치가 함께 폐기된다) 실패해도 보고 있던 화면을 유지한다.
         */
        fun refreshOnReturn() {
            // 진입 직후의 ON_RESUME 은 init 로드와 겹친다 — 진행 중이면 건너뛴다.
            // 컴포지션 쪽 플래그가 아니라 VM 이 들고 있는 Job 으로 판단해야
            // 프로세스 사망 후 복원(VM 재생성 + 플래그 복원)에서도 중복이 나지 않는다.
            if (loadJob?.isActive == true) return
            load(showsLoading = false, keepsStateOnFailure = true)
        }

        /** 조회 실패 화면의 재시도 — 로딩을 보여도 잃을 것이 없다(보고 있던 것이 오류 문구뿐). */
        fun retry() = load()

        fun delete(id: Long) {
            viewModelScope.launch {
                repository
                    .delete(id)
                    .onSuccess { load() }
                    // 실패를 무시하면 항목이 그대로 남은 채 아무 안내도 없어 고장처럼 보인다 (#716).
                    .onFailure {
                        internalState.update {
                            it.copy(deleteError = UiText.Resource(R.string.mindrecord_error_delete_failed))
                        }
                    }
            }
        }

        fun consumeDeleteError() {
            internalState.update { it.copy(deleteError = null) }
        }

        private fun load(
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (showsLoading) {
                        internalState.update { it.copy(loadPhase = LoadPhase.Loading) }
                    }

                    val today = repository.getToday().getOrNull()
                    val listResult = repository.getList()
                    // repository 가 runCatching 으로 감싸 CancellationException 까지 실패로 바꿔 돌려준다.
                    // 새 로드가 이 Job 을 취소했다면 상태는 그쪽이 결정하므로 여기서 멈춘다.
                    ensureActive()
                    // 서버는 draftOnly 없이 조회하면 임시저장을 제외해 내려주지만, 파라미터를 무시하는
                    // 서버를 만나도 임시저장이 답변 목록에 새지 않도록 한 겹 더 거른다.
                    val list = listResult.getOrNull().orEmpty().filter { !it.isDraft }

                    // 목록 조회 실패는 today 성공 여부와 무관하게 드러내야 한다. AND 로 묶으면
                    // today 만 성공했을 때 "답변 0개" 로 보여 실패가 화면에서 사라진다.
                    if (listResult.isFailure) {
                        val message =
                            UiText.Resource(R.string.mindrecord_error_daily_question_list_failed)
                        internalState.update { current ->
                            if (keepsStateOnFailure && current.loadPhase is LoadPhase.Loaded) {
                                current
                            } else {
                                current.copy(loadPhase = LoadPhase.Failed(message))
                            }
                        }
                    } else {
                        internalState.update { it.copy(loadPhase = LoadPhase.Loaded(today, list)) }
                    }
                }
        }

        private data class InternalState(
            val loadPhase: LoadPhase = LoadPhase.Loading,
            val deleteError: UiText? = null,
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
                        deleteError = deleteError,
                    )
                }

                is LoadPhase.Failed -> {
                    DailyQuestionListUiState.Error(phase.message)
                }
            }
    }
