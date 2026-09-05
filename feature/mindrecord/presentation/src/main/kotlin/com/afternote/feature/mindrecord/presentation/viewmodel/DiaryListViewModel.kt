package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DiaryList
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.domain.sync.MindRecordChangeTracker
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.reporting.MindRecordFailureStage
import com.afternote.feature.mindrecord.presentation.reporting.recordMindRecordFailure
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
        private val changeTracker: MindRecordChangeTracker,
        private val errorReporter: ErrorReporter,
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

        /** 마지막으로 성공한 조회 시점의 데이터 버전. 아직 성공한 적이 없으면 null. */
        private var loadedVersion: Long? = null

        /**
         * 탭 전환·`ON_RESUME` 등 사용자가 요청하지 않은 자동 갱신.
         *
         * **데이터가 바뀌었을 때만 다시 부른다** (#736). 진행 중인 로드는 종전대로 Job 으로
         * 막는다 — 컴포지션 쪽 플래그가 아니라 VM 이 들고 있는 값으로 판단해야 프로세스
         * 사망 후 복원에서도 중복이 나지 않는다.
         */
        fun refreshOnReturn() {
            if (loadJob?.isActive == true) return
            if (loadedVersion != null && loadedVersion == changeTracker.version) return
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
                    }.onFailure { throwable ->
                        // 실패를 무시하면 항목이 그대로 남은 채 아무 안내도 없어 고장처럼 보인다 (#716).
                        // 되돌릴 수 없는 동작이라 콘솔에도 남긴다 — 화면만 알리고 끝내면
                        // 나중에 「왜 안 지워졌나」를 물을 곳이 없다 (#964).
                        errorReporter.recordMindRecordFailure(MindRecordFailureStage.RECORD_DELETE, throwable)
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
                    // **조회를 시작하기 직전**의 버전을 잡아 둔다.
                    //
                    // 끝난 시점에 읽으면 조회와 겹친 쓰기를 통째로 삼킨다 — GET 이 서버
                    // snapshot 을 읽은 뒤 응답이 오는 사이에 create 가 성공하면, 이 결과에는
                    // 새 항목이 없는데도 증가한 최신 버전을 «내가 본 버전» 으로 기록한다.
                    // 그러면 복귀 시 두 값이 같아 재조회를 건너뛰고, 방금 저장한 항목이
                    // 목록에서 빠진 채 고정된다 (#736 리뷰).
                    val versionAtLoadStart = changeTracker.version

                    val listResult = repository.getList(yearMonth = yearMonth.toString(), draftOnly = null)
                    // 새 로드가 이 Job 을 취소했다면 상태는 그쪽이 결정하므로 여기서 멈춘다.
                    // repository 는 `runCatchingCancellable` 로 취소를 다시 던지므로 대개 여기 오기 전에
                    // 빠져나가지만, 조회가 끝난 뒤 취소가 들어온 경우를 위해 남겨 둔다.
                    ensureActive()
                    listResult
                        .onSuccess { result ->
                            loadedVersion = versionAtLoadStart
                            internalState.update {
                                // 목록을 새로 받아 왔으면 옛 삭제 실패 안내도 걷는다. 남겨 두면
                                // «새로 받아 왔는데 실패 안내는 그대로» 가 되어 #716 이 고치려는
                                // «고장처럼 보인다» 와 같은 성질이 된다 (리뷰 지적).
                                it.copy(loadPhase = LoadPhase.Loaded(result), deleteError = null)
                            }
                        }.onFailure { e ->
                            // **사용자가 오류 화면을 마주한 경우에만** 올린다. 재진입 갱신 실패는
                            // 보고 있던 목록을 그대로 두므로 승격하지 않는다 — 화면 이탈이 잦아
                            // 전부 올리면 한도를 잡음으로 채운다 (#964).
                            //
                            // 판정을 update 람다 **밖에서** 한다. `MutableStateFlow.update` 는 CAS
                            // 재시도 때 람다를 다시 평가하므로, 안에 두면 이중 보고가 될 자리다
                            // (지금은 쓰기가 전부 Main.immediate 라 재시도가 없다, #964 리뷰).
                            val showsErrorScreen =
                                !(keepsStateOnFailure && internalState.value.loadPhase is LoadPhase.Loaded)
                            if (showsErrorScreen) {
                                errorReporter.recordMindRecordFailure(MindRecordFailureStage.RECORD_LIST_LOAD, e)
                            }
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
