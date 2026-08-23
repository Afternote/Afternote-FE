package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.repository.DailyQuestionRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/**
 * 임시저장 목록 ViewModel.
 *
 * 두 카테고리 모두 백엔드가 제공하는 `draftOnly=true` 쿼리로 조회한다 — 전체를 받아 클라에서 거르면
 * 서버가 페이징을 붙였을 때 draft 가 뒤 페이지로 밀려 누락될 수 있다.
 *
 * 조회 범위는 일기가 이번 달 한정(`yearMonth`)인 반면 데일리질문은 전체 기간이다. 데일리질문 API 의
 * `date` 는 특정 하루만 받아 "이번 달" 을 표현할 수 없어 맞추지 못했다.
 */
@HiltViewModel
class DraftListViewModel
    @Inject
    constructor(
        private val diaryRepository: DiaryRepository,
        private val dailyQuestionRepository: DailyQuestionRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<DraftListUiState>(DraftListUiState.Loading)
        val uiState: StateFlow<DraftListUiState> = _uiState.asStateFlow()

        init {
            load()
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
                coroutineScope {
                    items
                        .map { item ->
                            async {
                                when (item.category) {
                                    DraftCategory.Diary -> diaryRepository.delete(item.id)

                                    DraftCategory.DailyQuestion -> dailyQuestionRepository.delete(item.id)

                                    // All 은 필터 라벨용 — 실제 항목 카테고리로는 등장하지 않는다.
                                    DraftCategory.All -> Result.success(Unit)
                                }
                            }
                        }.awaitAll()
                }
                val refreshed = collectDrafts()
                _uiState.value =
                    if (refreshed != null) {
                        DraftListUiState.Success(items = refreshed, deleteCompleted = true)
                    } else {
                        DraftListUiState.Error(UiText.Resource(R.string.mindrecord_error_generic))
                    }
            }
        }

        fun deleteAll() {
            val current = _uiState.value as? DraftListUiState.Success ?: return
            delete(current.items)
        }

        fun consumeDeleteCompleted() {
            _uiState.update {
                if (it is DraftListUiState.Success) it.copy(deleteCompleted = false) else it
            }
        }

        private fun load() {
            viewModelScope.launch {
                _uiState.value = DraftListUiState.Loading
                val items = collectDrafts()
                _uiState.value =
                    if (items != null) {
                        DraftListUiState.Success(items = items)
                    } else {
                        DraftListUiState.Error(UiText.Resource(R.string.mindrecord_error_generic))
                    }
            }
        }

        private suspend fun collectDrafts(): List<DraftItem>? =
            runCatching {
                coroutineScope {
                    val yearMonth = YearMonth.now().toString()
                    // 조회 실패는 예외로 올려 바깥 runCatching 이 잡게 한다.
                    // getOrNull().orEmpty() 로 흡수하면 "실패" 와 "0건" 이 같은 빈 화면이 되어
                    // Error 상태가 도달 불가능한 죽은 경로가 된다.
                    val draftDiariesDeferred =
                        async {
                            diaryRepository
                                .getList(yearMonth = yearMonth, draftOnly = true)
                                .getOrThrow()
                                .diaries
                        }
                    val draftDailyQuestionsDeferred =
                        async {
                            dailyQuestionRepository
                                .getList(draftOnly = true)
                                .getOrThrow()
                        }

                    val diaryItems =
                        // 날짜를 못 정한 항목은 toUi() 가 null 을 돌린다 — 정렬 키가 없어 뺀다.
                        draftDiariesDeferred.await().mapNotNull { diary ->
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
                        draftDailyQuestionsDeferred.await().map { question ->
                            val ui = question.toUi()
                            DraftItem(
                                id = ui.id,
                                category = DraftCategory.DailyQuestion,
                                title = ui.title,
                                content = ui.content,
                                date = ui.date,
                            )
                        }

                    (diaryItems + dailyQuestionItems).sortedByDescending { it.date }
                }
            }.getOrNull()
    }
