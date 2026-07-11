package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.ReceiverMindRecords
import com.afternote.feature.mindrecord.domain.repository.MindRecordReceiverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수신자 마음의 기록 화면 ViewModel.
 *
 * `receiver-auth` 의 데일리질문/일기/깊은생각 3개 엔드포인트를 병렬 조회해 탭별로 노출한다.
 * 정렬·기간 필터는 클라이언트에서 [ReceiverMindRecordFilter] 로 처리하고, 깊은생각 카테고리
 * 칩은 깊은생각 응답의 `categories` 를 그대로 사용해 실제 필터링한다.
 */
@HiltViewModel
class ReceiverMindRecordViewModel
    @Inject
    constructor(
        private val repository: MindRecordReceiverRepository,
    ) : ViewModel() {
        private val rawRecords = MutableStateFlow<ReceiverMindRecords?>(null)
        private val _uiState = MutableStateFlow<ReceiverMindRecordUiState>(ReceiverMindRecordUiState.Loading)
        val uiState: StateFlow<ReceiverMindRecordUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun refresh() = load()

        fun selectDeepThoughtCategory(category: String?) {
            _uiState.update { current ->
                if (current is ReceiverMindRecordUiState.Success) {
                    current.copy(selectedDeepThoughtCategory = category).withDerived()
                } else {
                    current
                }
            }
        }

        fun applyFilter(filter: ReceiverMindRecordFilter) {
            _uiState.update { current ->
                if (current is ReceiverMindRecordUiState.Success) {
                    current.copy(filter = filter).withDerived()
                } else {
                    current
                }
            }
        }

        fun resetFilter() = applyFilter(ReceiverMindRecordFilter())

        private fun load() {
            viewModelScope.launch {
                _uiState.update { ReceiverMindRecordUiState.Loading }
                repository
                    .getAll()
                    .onSuccess { records ->
                        rawRecords.value = records
                        _uiState.value =
                            ReceiverMindRecordUiState
                                .Success(
                                    dailyQuestions = emptyList(),
                                    diaries = emptyList(),
                                    deepThoughts = emptyList(),
                                    deepThoughtCategories = records.deepThoughtCategories,
                                ).withDerived()
                    }.onFailure { e ->
                        _uiState.value =
                            ReceiverMindRecordUiState.Error(
                                message = e.message ?: "마음의 기록을 불러오지 못했습니다.",
                            )
                    }
            }
        }

        /**
         * 필터/카테고리 선택 변경 시 파생 list 3종을 재계산해 반환한다.
         * 수신자 view 는 임시저장 record 를 보지 않는다 — 서버가 제외하기를 기대하나 방어적으로 한 번 더 거른다.
         */
        private fun ReceiverMindRecordUiState.Success.withDerived(): ReceiverMindRecordUiState.Success {
            val records = rawRecords.value ?: return this

            fun List<MindRecordSummary>.derived(): List<MindRecordSummary> =
                filterNot { it.isDraft }
                    .filterByDate(filter.fromDate, filter.toDate)
                    .sortBy(filter.sortOrder)

            val deepThoughts =
                records.deepThoughts.derived().let { list ->
                    val category = selectedDeepThoughtCategory
                    if (category == null) list else list.filter { it.category == category }
                }
            return copy(
                dailyQuestions = records.dailyQuestions.derived(),
                diaries = records.diaries.derived(),
                deepThoughts = deepThoughts,
            )
        }

        companion object {
            private fun List<MindRecordSummary>.filterByDate(
                from: String?,
                to: String?,
            ): List<MindRecordSummary> =
                filter { record ->
                    (from == null || record.recordDate >= from) && (to == null || record.recordDate <= to)
                }

            private fun List<MindRecordSummary>.sortBy(order: SortOrder): List<MindRecordSummary> =
                when (order) {
                    SortOrder.NEWEST -> sortedByDescending { it.recordDate }
                    SortOrder.OLDEST -> sortedBy { it.recordDate }
                }
        }
    }
