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

        /** 조회 실패 화면의 재시도 — 로딩을 보여도 잃을 것이 없다(보고 있던 것이 오류 문구뿐). */
        fun retry() = load(yearMonth = internalState.value.yearMonth)

        fun delete(id: Long) {
            viewModelScope.launch {
                repository
                    .delete(id)
                    .onSuccess {
                        // 지난 실패 문구를 함께 걷는다. 남겨 두면 «항목은 사라졌는데 실패
                        // 안내는 그대로» 인 화면이 VM 수명 내내 유지된다 (리뷰 지적).
                        internalState.update { it.copy(deleteError = null) }
                        // 삭제는 사용자가 요청했지만 뒤따르는 재조회는 아니다. 로딩을 방출하면
                        // 목록이 통째 교체되며 LazyColumn 스크롤이 맨 위로 돌아간다.
                        // 다만 재조회가 실패하면 삭제한 항목이 그대로 남아 보이므로 에러는 드러낸다.
                        load(internalState.value.yearMonth, showsLoading = false)
                    }.onFailure {
                        // 실패를 무시하면 항목이 그대로 남은 채 아무 안내도 없어 고장처럼 보인다 (#716).
                        internalState.update {
                            it.copy(deleteError = UiText.Resource(R.string.mindrecord_error_delete_failed))
                        }
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
                    // 새 로드가 이 Job 을 취소했다면 상태는 그쪽이 결정하므로 여기서 멈춘다.
                    // repository 는 `runCatchingCancellable` 로 취소를 다시 던지므로 대개 여기 오기 전에
                    // 빠져나가지만, 조회가 끝난 뒤 취소가 들어온 경우를 위해 남겨 둔다.
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
                                                UiText.Resource(R.string.mindrecord_error_diary_list_failed),
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
            val deleteError: UiText? = null,
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
                        // 임시저장은 캘린더 목록에 섞지 않는다. 서버는 `draftOnly` 를 생략하면
                        // 그 달 전체(임시저장 포함)를 내려주므로 여기서 걸러야 한다 — 같은 모듈의
                        // DailyQuestionListViewModel·ReceiverMindRecordViewModel 과 같은 규칙이다.
                        // 날짜를 못 정한 항목은 toUi() 가 null 을 돌려 함께 빠진다.
                        diaries =
                            phase.list.diaries
                                .filterNot { it.isDraft }
                                .mapNotNull { it.toUi() },
                        yearMonth = yearMonth,
                        monthDiaryCount = phase.list.monthDiaryCount,
                        weeklyDominantMood = phase.list.weeklyDominantMood,
                        deleteError = deleteError,
                    )
                }

                is LoadPhase.Failed -> {
                    DiaryListUiState.Error(phase.message)
                }
            }
    }
