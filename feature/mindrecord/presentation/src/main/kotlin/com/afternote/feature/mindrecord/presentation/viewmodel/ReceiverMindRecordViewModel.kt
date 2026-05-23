package com.afternote.feature.mindrecord.presentation.viewmodel

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
 * `GET /api/v1/receiver-auth/mind-records` 단일 응답을 받아 [MindRecordType] 로 그룹핑하고,
 * 클라이언트측 정렬/기간 필터를 적용해 [ReceiverMindRecordUiState.Success] 로 노출한다.
 * 발신자측이 카테고리별로 3 API 를 병렬 호출하는 것과 대조적으로, 수신자 API 는 단일 list
 * 엔드포인트만 제공되어 클라 분리 책임.
 *
 * 응답에 `senderName` 이 record 단위로 포함되어 수신자가 여러 발신자 기록을 *통합 조회* 하는
 * 형태이며, 카드에 발신자 이름을 표시해 출처를 명확히 한다.
 *
 * **깊은생각 카테고리 칩 (임시 mock)**: 수신자측 list 응답에는 `category` 가 없고, 별도
 * receiver-auth 카테고리 마스터 엔드포인트도 없다 (`/api/v1/deep-thought/categories` 는
 * sender 전용). 디자인 정합을 위해 [MOCK_DEEP_THOUGHT_CATEGORIES] 하드코딩 라벨로 칩 UI 만
 * 노출하고, 선택 시 record 필터링은 데이터 부재로 동작하지 않는다 (전체 list 그대로 노출).
 * 백엔드가 list 응답에 `category` 를 추가하거나 receiver-auth 카테고리 마스터 엔드포인트를
 * 신설하면 실데이터로 교체. Notion `Mind-Record 조회` 페이지에 누락 사항 기록.
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
                    // 백엔드가 category 를 내려주기 전까지 selection 은 UI 상태로만 보관.
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
                        // 수신자 view 는 임시저장 record 를 보지 않는다. 백엔드가 제외하기를 기대하나
                        // 방어적으로 한 번 더 거른다.
                        val visible = list.mindRecords.filterNot { it.isDraft }
                        rawRecords.value = visible
                        val initial =
                            ReceiverMindRecordUiState.Success(
                                dailyQuestions = emptyList(),
                                diaries = emptyList(),
                                deepThoughts = emptyList(),
                                deepThoughtCategories = MOCK_DEEP_THOUGHT_CATEGORIES,
                            )
                        _uiState.value = initial.withDerived(visible)
                    }.onFailure { e ->
                        _uiState.value =
                            ReceiverMindRecordUiState.Error(
                                message = e.message ?: "마음의 기록을 불러오지 못했습니다.",
                            )
                    }
            }
        }

        /**
         * `filter` 변경 시 파생 list 3종을 재계산해 반환한다.
         */
        private fun ReceiverMindRecordUiState.Success.withDerived(all: List<MindRecordSummary>): ReceiverMindRecordUiState.Success {
            val byDate = all.filterByDate(filter.fromDate, filter.toDate).sortBy(filter.sortOrder)
            return copy(
                dailyQuestions = byDate.filter { it.type == MindRecordType.DAILY_QUESTION },
                diaries = byDate.filter { it.type == MindRecordType.DIARY },
                deepThoughts = byDate.filter { it.type == MindRecordType.DEEP_THOUGHT },
            )
        }

        companion object {
            /**
             * 깊은생각 카테고리 칩 임시 mock 라벨. 백엔드 카테고리 마스터 API 가
             * receiver-auth 에 추가되면 실 데이터로 교체.
             *
             * 라벨은 sender 측 `/api/v1/deep-thought/categories` 응답 예시(`나의 가치관`,
             * `오늘 떠올린 생각`, `커리어`)와 디자인 노드 1727-19627 의 9개 칩 폭을 참조해
             * 9개 항목으로 구성.
             */
            private val MOCK_DEEP_THOUGHT_CATEGORIES: List<String> =
                listOf(
                    "나의 가치관",
                    "오늘 떠올린 생각",
                    "커리어",
                    "관계",
                    "가족",
                    "취미",
                    "여행",
                    "꿈",
                )

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
