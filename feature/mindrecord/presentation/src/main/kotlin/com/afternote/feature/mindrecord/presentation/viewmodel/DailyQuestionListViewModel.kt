package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DailyQuestion
import com.afternote.feature.mindrecord.domain.model.TodayDailyQuestion
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
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
import javax.inject.Inject

@HiltViewModel
class DailyQuestionListViewModel
    @Inject
    constructor(
        private val repository: DailyQuestionRepository,
        private val changeTracker: MindRecordChangeTracker,
        private val errorReporter: ErrorReporter,
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
         * 마지막으로 **완전히** 성공한 조회 시점의 데이터 버전. 아직 없으면 null.
         *
         * today 나 목록 중 하나라도 못 받아 온 로드는 여기 기록하지 않는다 — 기록하면
         * 못 받아 온 채로 복귀 재조회가 막힌다.
         */
        private var loadedVersion: Long? = null

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

        /** 조회 실패 화면의 재시도 — 로딩을 보여도 잃을 것이 없다(보고 있던 것이 오류 문구뿐). */
        fun retry() = load()

        fun delete(id: Long) {
            viewModelScope.launch {
                repository
                    .delete(id)
                    .onSuccess {
                        // 지난 실패 문구를 함께 걷는다. 남겨 두면 «항목은 사라졌는데 실패
                        // 안내는 그대로» 인 화면이 VM 수명 내내 유지된다 (리뷰 지적).
                        internalState.update { it.copy(deleteError = null) }
                        load()
                    }
                    // 실패를 무시하면 항목이 그대로 남은 채 아무 안내도 없어 고장처럼 보인다 (#716).
                    .onFailure { throwable ->
                        // 되돌릴 수 없는 동작이라 콘솔에도 남긴다 (#964).
                        errorReporter.recordMindRecordFailure(MindRecordFailureStage.RECORD_DELETE, throwable)
                        internalState.update {
                            it.copy(deleteError = UiText.Resource(R.string.mindrecord_error_delete_failed))
                        }
                    }
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

                    // **조회를 시작하기 직전**의 버전을 잡아 둔다.
                    //
                    // 끝난 시점에 읽으면 조회와 겹친 쓰기를 통째로 삼킨다 — GET 이 서버
                    // snapshot 을 읽은 뒤 응답이 오는 사이에 create 가 성공하면, 이 결과에는
                    // 새 항목이 없는데도 증가한 최신 버전을 «내가 본 버전» 으로 기록한다.
                    // 그러면 복귀 시 두 값이 같아 재조회를 건너뛰고, 방금 저장한 항목이
                    // 목록에서 빠진 채 고정된다 (#736 리뷰).
                    val versionAtLoadStart = changeTracker.version

                    val todayResult = repository.getToday()
                    val today = todayResult.getOrNull()
                    val listResult = repository.getList()
                    // 새 로드가 이 Job 을 취소했다면 상태는 그쪽이 결정하므로 여기서 멈춘다.
                    // repository 는 `runCatchingCancellable` 로 취소를 다시 던지므로 대개 여기 오기 전에
                    // 빠져나가지만, 조회가 끝난 뒤 취소가 들어온 경우를 위해 남겨 둔다.
                    ensureActive()
                    // 서버는 draftOnly 없이 조회하면 임시저장을 제외해 내려주지만, 파라미터를 무시하는
                    // 서버를 만나도 임시저장이 답변 목록에 새지 않도록 한 겹 더 거른다.
                    val list = listResult.getOrNull().orEmpty().filter { !it.isDraft }

                    // 목록 조회 실패는 today 성공 여부와 무관하게 드러내야 한다. AND 로 묶으면
                    // today 만 성공했을 때 "답변 0개" 로 보여 실패가 화면에서 사라진다.
                    if (listResult.isFailure) {
                        val message =
                            UiText.Resource(R.string.mindrecord_error_daily_question_list_failed)
                        // 오류 화면을 마주한 경우에만 올린다 — 재진입 갱신 실패는 보고 있던
                        // 목록을 그대로 두므로 승격하지 않는다 (#964).
                        //
                        // 판정을 update 람다 **밖에서** 한다. `MutableStateFlow.update` 는 CAS
                        // 재시도 때 람다를 다시 평가하므로 안에 두면 이중 보고가 될 자리다.
                        // 같은 파일 delete() 와 일기 쪽 DiaryListViewModel 도 이 모양이다 (#964 리뷰).
                        val showsErrorScreen =
                            !(keepsStateOnFailure && internalState.value.loadPhase is LoadPhase.Loaded)
                        if (showsErrorScreen) {
                            listResult.exceptionOrNull()?.let { throwable ->
                                errorReporter.recordMindRecordFailure(
                                    MindRecordFailureStage.RECORD_LIST_LOAD,
                                    throwable,
                                )
                            }
                        }
                        internalState.update { current ->
                            if (keepsStateOnFailure && current.loadPhase is LoadPhase.Loaded) {
                                current
                            } else {
                                current.copy(loadPhase = LoadPhase.Failed(message))
                            }
                        }
                    } else {
                        // **today 실패는 «본 적 있다» 로 기록하지 않는다** (#736 리뷰).
                        //
                        // today 는 실패해도 화면을 막지 않는다 — 배너만 빠지고 답변 목록은 그대로
                        // 쓸 수 있다. 그러나 그때 버전까지 찍어 두면 복귀할 때마다 «본 버전과 같다» 로
                        // 재조회를 건너뛰어, **배너가 사라진 채 VM 수명 내내 고정된다.** 서버가
                        // 회복돼도 돌아오지 않는다.
                        //
                        // 그래서 today 가 실패한 로드는 미완으로 두어 다음 복귀가 다시 부르게 한다.
                        // #736 이 줄이려는 것은 «달라진 게 없는데 또 부르는» 요청이지, 아직 못 받아
                        // 온 것을 다시 받아 오는 요청이 아니다.
                        loadedVersion = versionAtLoadStart.takeIf { todayResult.isSuccess }
                        internalState.update {
                            // 목록을 새로 받아 왔으면 옛 삭제 실패 안내도 걷는다. 남겨 두면
                            // «새로 받아 왔는데 실패 안내는 그대로» 가 되어 #716 이 고치려는
                            // «고장처럼 보인다» 와 같은 성질이 된다 (리뷰 지적).
                            it.copy(loadPhase = LoadPhase.Loaded(today, list), deleteError = null)
                        }
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
                                    day = it.day,
                                    content = it.content,
                                    isAnswered = it.isAnswered,
                                )
                            },
                        // 날짜를 못 정한 항목은 toUi() 가 null 을 돌린다 — 정렬 키가 없어 카드로 만들지 않는다 (#751).
                        answers = phase.answers.mapNotNull { it.toUi() },
                        deleteError = deleteError,
                    )
                }

                is LoadPhase.Failed -> {
                    DailyQuestionListUiState.Error(phase.message)
                }
            }
    }
