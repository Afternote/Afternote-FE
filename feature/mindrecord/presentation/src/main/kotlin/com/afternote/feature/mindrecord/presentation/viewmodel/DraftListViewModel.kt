package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import com.afternote.feature.mindrecord.domain.repository.DiaryRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

/**
 * 임시저장 목록 ViewModel.
 *
 * - 일기: 백엔드가 `draftOnly=true` 쿼리를 제공해 그대로 호출 (단, 이번 달 한정).
 * - 깊은 생각: 전체 리스트를 받아 `isDraft = true` client-side 필터 (전용 쿼리 없음).
 * - 데일리질문: 응답에 `isDraft` 가 없어 분류 불가 → 0건 처리 (TODO).
 *
 * 두 호출은 병렬로 묶고, 어느 한쪽이 실패해도 다른 쪽은 비어 있는 결과로 합쳐 화면이 깨지지 않게 한다.
 *
 * 선택 모드에서 전체/선택 삭제 시 항목별 delete API를 병렬 호출하고, 성공한 항목만 목록에서 제거한다.
 */
@HiltViewModel
class DraftListViewModel
    @Inject
    constructor(
        private val diaryRepository: DiaryRepository,
        private val deepThoughtRepository: DeepThoughtRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<DraftListUiState>(DraftListUiState.Loading)
        val uiState: StateFlow<DraftListUiState> = _uiState.asStateFlow()

        /** 삭제 완료 토스트("임시 저장된 기록이 삭제 되었습니다") 노출 이벤트. */
        private val _deletedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val deletedEvent: SharedFlow<Unit> = _deletedEvent.asSharedFlow()

        init {
            load()
        }

        fun refresh() = load()

        /** 상단 "선택"/"완료" 버튼 — 선택 모드를 토글하고 종료 시 선택을 초기화한다. */
        fun onSelectionModeToggled() {
            _uiState.update { state ->
                if (state is DraftListUiState.Success) {
                    if (state.selectionMode) {
                        state.copy(selectionMode = false, selectedKeys = emptySet())
                    } else {
                        state.copy(selectionMode = true)
                    }
                } else {
                    state
                }
            }
        }

        fun onItemCheckedChanged(item: DraftItem) {
            _uiState.update { state ->
                if (state is DraftListUiState.Success) {
                    val selected =
                        if (item.key in state.selectedKeys) {
                            state.selectedKeys - item.key
                        } else {
                            state.selectedKeys + item.key
                        }
                    state.copy(selectedKeys = selected)
                } else {
                    state
                }
            }
        }

        fun onDeleteAllClick() {
            val state = _uiState.value as? DraftListUiState.Success ?: return
            deleteItems(state.items)
        }

        fun onDeleteSelectedClick() {
            val state = _uiState.value as? DraftListUiState.Success ?: return
            deleteItems(state.items.filter { it.key in state.selectedKeys })
        }

        private fun deleteItems(targets: List<DraftItem>) {
            if (targets.isEmpty()) return
            viewModelScope.launch {
                val deletedKeys =
                    coroutineScope {
                        targets
                            .map { item ->
                                async {
                                    val result =
                                        when (item.category) {
                                            DraftCategory.Diary -> diaryRepository.delete(item.id)

                                            DraftCategory.DeepThought -> deepThoughtRepository.delete(item.id)

                                            // 데일리질문은 isDraft 미노출로 목록에 없음 — 방어적으로 통과 처리.
                                            DraftCategory.DailyQuestion -> Result.success(Unit)
                                        }
                                    item.key.takeIf { result.isSuccess }
                                }
                            }.awaitAll()
                            .filterNotNull()
                            .toSet()
                    }
                if (deletedKeys.isEmpty()) return@launch
                _uiState.update { state ->
                    if (state is DraftListUiState.Success) {
                        state.copy(
                            items = state.items.filterNot { it.key in deletedKeys },
                            selectedKeys = state.selectedKeys - deletedKeys,
                        )
                    } else {
                        state
                    }
                }
                _deletedEvent.tryEmit(Unit)
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
                    val deepThoughtDeferred =
                        async {
                            deepThoughtRepository
                                .getList()
                                .getOrNull()
                                ?.items
                                ?.filter { it.isDraft }
                                .orEmpty()
                        }

                    val diaryItems =
                        diaryDeferred.await().map { diary ->
                            val ui = diary.toUi()
                            DraftItem(
                                id = ui.id,
                                category = DraftCategory.Diary,
                                content = ui.content,
                                date = ui.date,
                            )
                        }
                    val deepThoughtItems =
                        deepThoughtDeferred.await().map { thought ->
                            val ui = thought.toUi()
                            DraftItem(
                                id = ui.id,
                                category = DraftCategory.DeepThought,
                                content = ui.content,
                                date = ui.date,
                            )
                        }

                    (diaryItems + deepThoughtItems).sortedByDescending { it.date }
                }
            }.getOrNull()
    }
