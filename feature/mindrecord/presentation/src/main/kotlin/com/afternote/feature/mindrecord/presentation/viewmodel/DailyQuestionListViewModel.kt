package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
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
        private val changeTracker: MindRecordChangeTracker,
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
         * 탭 전환·`ON_RESUME` 등 사용자가 요청하지 않은 자동 갱신.
         *
         * **데이터가 바뀌었을 때만 다시 부른다** (#736). 종전에는 돌아오기만 하면 무조건
         * 재조회해, 화면 off/on 이나 진입 직후의 `ON_RESUME` 까지 같은 요청을 한 번 더
         * 내보냈다 — 마음의 기록 첫 진입 한 번에 요청이 7건 나간 원인 중 하나다.
         *
         * 진행 중인 로드는 종전대로 Job 으로 막는다. 컴포지션 쪽 플래그가 아니라 VM 이
         * 들고 있는 값으로 판단해야 프로세스 사망 후 복원에서도 중복이 나지 않는다.
         */
        fun refreshOnReturn() {
            if (loadJob?.isActive == true) return
            if (loadedVersion != null && loadedVersion == changeTracker.version) return
            load(showsLoading = false, keepsStateOnFailure = true)
        }

        /** 마지막으로 성공한 조회 시점의 데이터 버전. 아직 성공한 적이 없으면 null. */
        private var loadedVersion: Long? = null

        fun delete(id: Long) {
            viewModelScope.launch {
                repository.delete(id).onSuccess { load() }
            }
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
                            UiText.DynamicOrResource(
                                value = listResult.exceptionOrNull()?.message,
                                fallbackResId = R.string.mindrecord_error_daily_question_list_failed,
                            )
                        internalState.update { current ->
                            if (keepsStateOnFailure && current.loadPhase is LoadPhase.Loaded) {
                                current
                            } else {
                                current.copy(loadPhase = LoadPhase.Failed(message))
                            }
                        }
                    } else {
                        // 조회를 시작한 시점이 아니라 **끝난 시점**의 버전을 기록한다.
                        // 조회 도중 들어온 변경을 놓치지 않기 위해서다.
                        loadedVersion = changeTracker.version
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
