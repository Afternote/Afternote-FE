package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.reporting.MindRecordFailureStage
import com.afternote.feature.mindrecord.presentation.reporting.recordMindRecordFailure
import com.afternote.feature.mindrecord.presentation.usecase.DeleteMindRecordDraftsUseCase
import com.afternote.feature.mindrecord.presentation.usecase.LoadMindRecordDraftsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 임시저장 목록 ViewModel.
 *
 * 조회 범위는 [LoadMindRecordDraftsUseCase] 가 정본으로 들고 있다 — 작성 툴바의 카운트가 같은 범위를
 * 봐야 목록 건수와 어긋나지 않는다 (#769).
 */
@HiltViewModel
class DraftListViewModel
    @Inject
    constructor(
        private val loadDrafts: LoadMindRecordDraftsUseCase,
        private val deleteDrafts: DeleteMindRecordDraftsUseCase,
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

                // «무엇을 지웠는지·무엇이 남았는지» 판정은 [DeleteMindRecordDraftsUseCase] 가 갖는다.
                // 여기 남는 것은 「그 판정을 화면의 어떤 상태로 보일까」 뿐이다 (#1693).
                var refreshed: List<DraftItem>? = null
                val outcome =
                    deleteDrafts.delete(
                        targets = items.mapNotNull { it.toDeleteTarget() },
                        survivorsAfterDelete = {
                            refreshed = collectDrafts()
                            refreshed?.mapNotNullTo(mutableSetOf()) { it.toDeleteTarget() }
                        },
                        // 되돌릴 수 없는 동작이라 실패는 콘솔에도 남겨야 한다 — 화면만 알리고
                        // 끝내면 나중에 「왜 안 지워졌나」를 물을 곳이 없다 (#964). 화면에 안
                        // 보이는 실패(이미 사라진 항목의 404)까지 기록한다 — 그쪽일수록 계측이
                        // 유일한 흔적이다.
                        //
                        // **재조회 전에** 부르는 훅이다. 반환값으로 받아 여기서 기록하면 재조회
                        // 도중 스코프가 취소될 때 삭제는 반영됐는데 흔적이 안 남는다 (#1693 리뷰).
                        onFailures = { failures ->
                            failures.forEach { failure ->
                                errorReporter.recordMindRecordFailure(
                                    MindRecordFailureStage.DRAFT_DELETE,
                                    failure.cause,
                                )
                            }
                        },
                    )

                // 재조회 결과가 없으면 **현 목록을 유지한다**. UseCase 는 targets 가 비면
                // survivorsAfterDelete 를 부르지 않고 Deleted 를 돌리므로 `refreshed` 가 null 인
                // 채 여기 올 수 있다 — orEmpty() 로 두면 목록이 빈 리스트로 갈린다. 지금은 위
                // items.isEmpty() 가드가 막고 있지만 그 가드에 기대지 않는다 (#1693 리뷰).
                val loaded = refreshed ?: (_uiState.value as? DraftListUiState.Success)?.items
                _uiState.value =
                    when (outcome) {
                        is DeleteMindRecordDraftsUseCase.Outcome.Deleted -> {
                            DraftListUiState.Success(
                                items = loaded.orEmpty(),
                                // 화면은 **남은 것**만 본다 — 이미 사라진 항목의 404 까지 «실패» 로
                                // 보이면 사용자가 다시 지울 수 없는 것을 다시 고르게 된다.
                                // 계측은 onFailures 훅이 재조회 전에 전건을 이미 올렸다 (#1693).
                                deleteOutcome =
                                    if (outcome.remaining.isEmpty()) {
                                        DraftDeleteOutcome.AllDeleted
                                    } else {
                                        DraftDeleteOutcome.SomeFailed(
                                            outcome.remaining.mapNotNull { failure -> failure.target.toDraftItem(loaded) },
                                        )
                                    },
                            )
                        }

                        // 재조회가 실패해 무엇이 남았는지 모른다 — 목록을 그릴 수 없다.
                        is DeleteMindRecordDraftsUseCase.Outcome.Unknown -> {
                            DraftListUiState.Error(UiText.Resource(R.string.mindrecord_error_generic))
                        }
                    }
            }
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
            loadDrafts
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

        /** 화면 항목 → 삭제 대상. «전체» 는 필터 라벨이라 지울 대상이 아니다. */
        private fun DraftItem.toDeleteTarget(): DeleteMindRecordDraftsUseCase.Target? =
            when (category) {
                DraftCategory.Diary -> {
                    DeleteMindRecordDraftsUseCase.Target(DeleteMindRecordDraftsUseCase.Category.Diary, id)
                }

                DraftCategory.DailyQuestion -> {
                    DeleteMindRecordDraftsUseCase.Target(DeleteMindRecordDraftsUseCase.Category.DailyQuestion, id)
                }

                DraftCategory.All -> {
                    null
                }
            }

        /**
         * 실패한 대상을 화면이 다시 선택해 줄 수 있는 항목으로 되돌린다.
         *
         * 재조회 목록에서 같은 키를 찾는다 — 그쪽이 서버가 방금 돌려준 최신 제목·본문이다.
         * UseCase 가 «아직 남아 있는» 실패만 올리므로 못 찾는 경우는 없지만, 그렇더라도
         * 목록 자체는 그려야 하므로 그 항목만 조용히 뺀다.
         */
        private fun DeleteMindRecordDraftsUseCase.Target.toDraftItem(refreshed: List<DraftItem>?): DraftItem? =
            refreshed.orEmpty().firstOrNull { it.toDeleteTarget() == this }
    }
