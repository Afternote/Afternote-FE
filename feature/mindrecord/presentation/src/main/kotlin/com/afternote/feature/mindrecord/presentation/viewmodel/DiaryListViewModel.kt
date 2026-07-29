package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
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
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DiaryListViewModel
    @Inject
    constructor(
        private val repository: DiaryRepository,
    ) : ViewModel() {
        private val internalState = MutableStateFlow(InternalState())
        private var loadJob: Job? = null

        val uiState: StateFlow<DiaryListUiState> =
            internalState
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DiaryListUiState.Loading,
                )

        init {
            load(internalState.value.yearMonth)
        }

        /**
         * 사용자가 캘린더 월을 바꿨을 때의 로드. 요청한 동작이므로 로딩을 노출하고
         * 실패하면 에러 화면으로 알린다.
         */
        fun selectYearMonth(yearMonth: YearMonth) {
            load(yearMonth)
        }

        /**
         * 탭 전환·작성 화면 복귀 등 사용자가 요청하지 않은 자동 갱신.
         *
         * [selectYearMonth] 와 세 가지가 다르다.
         * - 로딩을 방출하지 않는다. 화면이 살아 있는 채로 발화하므로 콘텐츠가 통째 교체되면
         *   캘린더 월과 스크롤 위치가 함께 폐기된다.
         * - 실패해도 보고 있던 화면을 유지한다.
         * - 보고 있던 월을 그대로 다시 조회한다. 6월을 보는 중에 7월 데이터를 받으면 안 된다.
         */
        fun refreshOnReturn() {
            // 진입 직후의 ON_RESUME 은 init 로드와 겹친다 — 진행 중이면 건너뛴다.
            // 컴포지션 쪽 플래그가 아니라 VM 이 들고 있는 Job 으로 판단해야
            // 프로세스 사망 후 복원(VM 재생성 + 플래그 복원)에서도 중복이 나지 않는다.
            if (loadJob?.isActive == true) return
            load(
                yearMonth = internalState.value.yearMonth,
                showsLoading = false,
                keepsStateOnFailure = true,
            )
        }

        fun delete(id: Long) {
            viewModelScope.launch {
                repository.delete(id).onSuccess {
                    // 삭제는 사용자가 요청했지만 뒤따르는 재조회는 아니다. 로딩을 방출하면
                    // 목록이 통째 교체되며 LazyColumn 스크롤이 맨 위로 돌아간다.
                    // 다만 재조회가 실패하면 삭제한 항목이 그대로 남아 보이므로 에러는 드러낸다.
                    load(internalState.value.yearMonth, showsLoading = false)
                }
            }
        }

        private fun load(
            yearMonth: YearMonth,
            showsLoading: Boolean = true,
            keepsStateOnFailure: Boolean = false,
        ) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    internalState.update {
                        it.copy(
                            yearMonth = yearMonth,
                            loadPhase = if (showsLoading) LoadPhase.Loading else it.loadPhase,
                        )
                    }
                    val listResult = repository.getList(yearMonth = yearMonth.toString(), draftOnly = null)
                    // repository 가 runCatching 으로 감싸 CancellationException 까지 실패로 바꿔 돌려준다.
                    // 새 로드가 이 Job 을 취소했다면 상태는 그쪽이 결정하므로 여기서 멈춘다.
                    ensureActive()
                    listResult
                        .onSuccess { result ->
                            internalState.update { it.copy(loadPhase = LoadPhase.Loaded(result)) }
                        }.onFailure { e ->
                            internalState.update { current ->
                                if (keepsStateOnFailure && current.loadPhase is LoadPhase.Loaded) {
                                    current
                                } else {
                                    current.copy(
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
        }

        private data class InternalState(
            // 조회 중인 월. 캘린더가 들고 있으면 자동 갱신이 어느 월을 다시 조회할지 알 수 없고,
            // 로딩으로 콘텐츠가 교체될 때 함께 폐기된다.
            val yearMonth: YearMonth = YearMonth.now(),
            val loadPhase: LoadPhase = LoadPhase.Loading,
        )

        private sealed interface LoadPhase {
            data object Loading : LoadPhase

            data class Loaded(
                val list: DiaryList,
            ) : LoadPhase

            data class Failed(
                val message: UiText,
            ) : LoadPhase
        }

        private fun InternalState.toUiState(): DiaryListUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> {
                    DiaryListUiState.Loading
                }

                is LoadPhase.Loaded -> {
                    DiaryListUiState.Success(
                        diaries = phase.list.diaries.map { it.toUi() },
                        yearMonth = yearMonth,
                        monthDiaryCount = phase.list.monthDiaryCount,
                        weeklyDominantMood = phase.list.weeklyDominantMood,
                    )
                }

                is LoadPhase.Failed -> {
                    DiaryListUiState.Error(phase.message)
                }
            }
    }
