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
 * - 일기: 백엔드가 `draftOnly=true` 쿼리를 제공해 그대로 호출 (단, 이번 달 한정).
 * - 데일리질문: 응답에 `isDraft` 가 없어 분류 불가 → 0건 처리 (TODO).
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

        fun refresh() = load()

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
                    val diaryDeferred =
                        async {
                            diaryRepository
                                .getList(yearMonth = yearMonth, draftOnly = true)
                                .getOrNull()
                                ?.diaries
                                .orEmpty()
                        }

                    val diaryItems =
                        diaryDeferred.await().map { diary ->
                            val ui = diary.toUi()
                            DraftItem(
                                id = ui.id,
                                category = DraftCategory.Diary,
                                title = ui.title,
                                content = ui.content,
                                date = ui.date,
                            )
                        }

                    diaryItems.sortedByDescending { it.date }
                }
            }.getOrNull()
    }
