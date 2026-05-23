package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.MindRecordSummary

/**
 * 수신자 마음의 기록 화면 — 단일 list 응답을 type 으로 그룹핑한 후 3개 탭에 분배한다.
 *
 * 정렬은 클라이언트에서 [SortOrder] 로 처리하며 필터(`filter`)가 적용된 경우 헤더에 인디케이터 노출.
 */
sealed interface ReceiverMindRecordUiState {
    data object Loading : ReceiverMindRecordUiState

    data class Success(
        val dailyQuestions: List<MindRecordSummary>,
        val diaries: List<MindRecordSummary>,
        val deepThoughts: List<MindRecordSummary>,
        val deepThoughtCategories: List<String>,
        val selectedDeepThoughtCategory: String? = null,
        val filter: ReceiverMindRecordFilter = ReceiverMindRecordFilter(),
    ) : ReceiverMindRecordUiState

    data class Error(
        val message: String,
    ) : ReceiverMindRecordUiState
}

/**
 * 필터 바텀시트 상태. 모두 null/UNSET 이면 "적용되지 않음" 으로 간주해 헤더에 인디케이터 미노출.
 */
data class ReceiverMindRecordFilter(
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val fromDate: String? = null,
    val toDate: String? = null,
) {
    val isApplied: Boolean = fromDate != null || toDate != null
}

enum class SortOrder {
    NEWEST,
    OLDEST,
    ;

    val label: String
        get() =
            when (this) {
                NEWEST -> "최신순"
                OLDEST -> "과거순"
            }
}
