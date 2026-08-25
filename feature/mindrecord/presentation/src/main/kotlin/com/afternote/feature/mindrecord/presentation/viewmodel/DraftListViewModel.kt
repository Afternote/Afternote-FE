package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.reporting.MindRecordFailureStage
import com.afternote.feature.mindrecord.presentation.reporting.recordMindRecordFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 임시저장 목록 ViewModel.
 *
 * 조회 범위는 [MindRecordDraftLoader] 가 정본으로 들고 있다 — 작성 툴바의 카운트가 같은 범위를
 * 봐야 목록 건수와 어긋나지 않는다 (#769).
 */
@HiltViewModel
class DraftListViewModel
    @Inject
    constructor(
        private val loader: MindRecordDraftLoader,
        private val diaryRepository: DiaryRepository,
        private val dailyQuestionRepository: DailyQuestionRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<DraftListUiState>(DraftListUiState.Loading)
        val uiState: StateFlow<DraftListUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            load()
        }

        /**
         * 화면 재진입 갱신.
         *
         * 로딩을 방출하지 않는다 — ON_RESUME 은 화면이 살아 있는 채로 발화하므로, 스피너로
         * 갈아치우면 보고 있던 목록과 선택 상태가 사라진다. 실패해도 기존 목록을 유지한다.
         * 마인드레코드 홈의 `refreshOnReturn` 과 같은 규칙이다.
         */
        fun refreshOnReturn() {
            // 진입 직후의 ON_RESUME 은 init 로드와 겹친다 — 진행 중이면 건너뛴다.
            if (loadJob?.isActive == true) return
            load(showsLoading = false)
        }

        /** 조회 실패 화면의 재시도 — 로딩을 보여도 잃을 것이 없다(보고 있던 것이 오류 문구뿐). */
        fun retry() = load()

        /** 선택 삭제 — 삭제 후 목록을 다시 불러오고 완료 토스트 노출 플래그를 세운다. */
        fun delete(items: List<DraftItem>) {
            val current = _uiState.value as? DraftListUiState.Success ?: return
            if (items.isEmpty() || current.isDeleting) return

            viewModelScope.launch {
                _uiState.update {
                    if (it is DraftListUiState.Success) it.copy(isDeleting = true) else it
                }
                // 항목별 결과를 항목과 짝지어 받는다 — 무엇이 실패했는지 알아야 다시 선택해 줄 수 있다.
                val results =
                    coroutineScope {
                        items
                            .map { item -> async { item to deleteOne(item) } }
                            .awaitAll()
                    }
                val failed = results.filter { (_, result) -> result.isFailure }.map { (item, _) -> item }
                // 되돌릴 수 없는 동작이고, 부분 실패는 목록과 서버 상태를 어긋나게 둔다 —
                // 화면은 사용자에게 알리지만 콘솔에는 아무 흔적도 남지 않았다 (#964).
                results.forEach { (_, result) ->
                    result.exceptionOrNull()?.let { throwable ->
                        errorReporter.recordMindRecordFailure(MindRecordFailureStage.DRAFT_DELETE, throwable)
                    }
                }

                val refreshed = collectDrafts()
                _uiState.value =
                    if (refreshed != null) {
                        // 실패했지만 재조회에서도 사라진 항목은 알리지 않는다 — 이미 없는 것을 지우려다
                        // 404 가 난 경우가 여기다. 사용자가 원한 결과는 이뤄졌고, 남아 있지도 않은 항목을
                        // 다시 선택해 주면 "목록은 비었는데 1개 선택" 같은 상태가 된다.
                        val remainingKeys = refreshed.mapTo(mutableSetOf()) { it.category to it.id }
                        val actionableFailures = failed.filter { (it.category to it.id) in remainingKeys }

                        DraftListUiState.Success(
                            items = refreshed,
                            deleteOutcome =
                                if (actionableFailures.isEmpty()) {
                                    DraftDeleteOutcome.AllDeleted
                                } else {
                                    DraftDeleteOutcome.SomeFailed(actionableFailures)
                                },
                        )
                    } else {
                        DraftListUiState.Error(UiText.Resource(R.string.mindrecord_error_generic))
                    }
            }
        }

        private suspend fun deleteOne(item: DraftItem): Result<Unit> =
            when (item.category) {
                DraftCategory.Diary -> diaryRepository.delete(item.id)

                DraftCategory.DailyQuestion -> dailyQuestionRepository.delete(item.id)

                // All 은 필터 라벨용 — 실제 항목 카테고리로는 등장하지 않는다.
                DraftCategory.All -> Result.success(Unit)
            }

        fun deleteAll() {
            val current = _uiState.value as? DraftListUiState.Success ?: return
            delete(current.items)
        }

        fun consumeDeleteOutcome() {
            _uiState.update {
                if (it is DraftListUiState.Success) it.copy(deleteOutcome = null) else it
            }
        }

        private fun load(showsLoading: Boolean = true) {
            loadJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    if (showsLoading) _uiState.value = DraftListUiState.Loading
                    val items = collectDrafts()
                    _uiState.value =
                        when {
                            items != null -> DraftListUiState.Success(items = items)

                            // 재진입 갱신이 실패하면 보고 있던 목록을 그대로 둔다 — 화면을 오류로
                            // 갈아치우면 사용자가 하던 일(선택·스크롤)이 사라진다.
                            !showsLoading -> _uiState.value

                            else -> DraftListUiState.Error(UiText.Resource(R.string.mindrecord_error_generic))
                        }
                }
        }

        private suspend fun collectDrafts(): List<DraftItem>? =
            loader
                .load()
                // 사용자가 «쓰다 만 글» 을 찾으러 들어오는 화면이라, 실패하면 작성물이 사라진
                // 것처럼 보인다 — #519 가 실제로 그 형태의 결함이었다 (#964).
                .onFailure { throwable ->
                    errorReporter.recordMindRecordFailure(MindRecordFailureStage.DRAFT_LIST_LOAD, throwable)
                }.map { drafts ->
                    val diaryItems =
                        // 날짜를 못 정한 항목은 toUi() 가 null 을 돌린다 — 정렬 키가 없어 뺀다.
                        drafts.diaries.mapNotNull { diary ->
                            val ui = diary.toUi() ?: return@mapNotNull null
                            DraftItem(
                                id = ui.id,
                                category = DraftCategory.Diary,
                                title = ui.title,
                                content = ui.content,
                                date = ui.date,
                            )
                        }
                    val dailyQuestionItems =
                        // 날짜를 못 정한 항목은 toUi() 가 null 을 돌린다 — 정렬 키가 없어 뺀다 (#751).
                        drafts.dailyQuestions.mapNotNull { question ->
                            val ui = question.toUi() ?: return@mapNotNull null
                            DraftItem(
                                id = ui.id,
                                category = DraftCategory.DailyQuestion,
                                title = ui.title,
                                content = ui.content,
                                date = ui.date,
                            )
                        }
                    (diaryItems + dailyQuestionItems).sortedByDescending { it.date }
                }.getOrNull()
    }
