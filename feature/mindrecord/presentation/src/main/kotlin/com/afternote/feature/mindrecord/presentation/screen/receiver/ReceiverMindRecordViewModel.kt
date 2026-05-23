package com.afternote.feature.mindrecord.presentation.screen.receiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary
import com.afternote.feature.mindrecord.domain.model.MindRecordType
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
 * `GET /receiver-auth/mind-records` 단일 응답을 받아 `MindRecordType` 으로 그룹핑하고,
 * 깊은생각 카테고리 칩과 클라이언트측 정렬/기간 필터를 적용해 [ReceiverMindRecordUiState.Success]
 * 로 노출한다. 발신자측이 카테고리별로 3 API 를 병렬 호출하는 것과 대조적으로, 수신자 API 는
 * 단일 list 엔드포인트만 제공되어 클라 분리 책임.
 */
@HiltViewModel
class ReceiverMindRecordViewModel
    @Inject
    constructor(
        private val repository: MindRecordReceiverRepository,
    ) : ViewModel() {
        private val rawRecords = MutableStateFlow<List<MindRecordSummary>>(emptyList())
        private val _uiState = MutableStateFlow<ReceiverMindRecordUiState>(ReceiverMindRecordUiState.Loading)
        val uiState: StateFlow<ReceiverMindRecordUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun refresh() = load()

        fun selectDeepThoughtCategory(category: String?) {
            _uiState.update { current ->
                if (current is ReceiverMindRecordUiState.Success) {
                    current.copy(selectedDeepThoughtCategory = category)
                } else {
                    current
                }
            }
        }

        fun applyFilter(filter: ReceiverMindRecordFilter) {
            _uiState.update { current ->
                if (current is ReceiverMindRecordUiState.Success) {
                    current.copy(filter = filter).withDerived(rawRecords.value)
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
                    .getList()
                    .onSuccess { list ->
                        rawRecords.value = list.mindRecords
                        val initial =
                            ReceiverMindRecordUiState.Success(
                                dailyQuestions = emptyList(),
                                diaries = emptyList(),
                                deepThoughts = emptyList(),
                                deepThoughtCategories = list.mindRecords.distinctDeepThoughtCategories(),
                            )
                        _uiState.value = initial.withDerived(list.mindRecords)
                    }.onFailure { e ->
                        _uiState.value =
                            ReceiverMindRecordUiState.Error(
                                message = e.message ?: "마음의 기록을 불러오지 못했습니다.",
                            )
                    }
            }
        }

        /**
         * `filter`/`selectedDeepThoughtCategory` 변경 시 파생 list 3종을 재계산해 반환한다.
         */
        private fun ReceiverMindRecordUiState.Success.withDerived(all: List<MindRecordSummary>): ReceiverMindRecordUiState.Success {
            val byDate = all.filterByDate(filter.fromDate, filter.toDate).sortBy(filter.sortOrder)
            val deep = byDate.filter { it.type == MindRecordType.DEEP_THOUGHT }
            return copy(
                dailyQuestions = byDate.filter { it.type == MindRecordType.DAILY_QUESTION },
                diaries = byDate.filter { it.type == MindRecordType.DIARY },
                deepThoughts =
                    selectedDeepThoughtCategory?.let { selected ->
                        // category 가 도메인 model 에 없으므로 stub: title prefix 매칭으로 대체.
                        // 실제 백엔드 카테고리 마스터 확정 시 [MindRecordSummary] 에 category 필드 추가 후 교체.
                        deep.filter { it.title.startsWith(selected) }
                    } ?: deep,
            )
        }

        companion object {
            private fun List<MindRecordSummary>.distinctDeepThoughtCategories(): List<String> =
                filter { it.type == MindRecordType.DEEP_THOUGHT }
                    .mapNotNull { it.title.substringBefore('|', missingDelimiterValue = "").ifBlank { null } }
                    .distinct()

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
