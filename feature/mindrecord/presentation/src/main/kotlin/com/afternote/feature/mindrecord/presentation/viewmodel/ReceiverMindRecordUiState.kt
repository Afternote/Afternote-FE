package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary

/**
 * 수신자 마음의 기록 화면 — `receiver-auth` 2개 엔드포인트 응답을 탭별로 분배한다.
 *
 * 정렬은 클라이언트에서 [SortOrder] 로 처리하며 필터(`filter`)가 적용된 경우 헤더에 인디케이터 노출.
 */
sealed interface ReceiverMindRecordUiState {
    data object Loading : ReceiverMindRecordUiState

    data class Success(
        val dailyQuestions: List<MindRecordSummary>,
        val diaries: List<MindRecordSummary>,
        val filter: ReceiverMindRecordFilter = ReceiverMindRecordFilter(),
    ) : ReceiverMindRecordUiState

    /**
     * 조회 실패.
     *
     * [message] 는 **도메인 문구**다. 종전에는 서버 응답의 `message` 를 그대로 실어
     * "아직 전달 조건이 충족되지 않았습니다" 같은 원문이 화면 문구가 됐다 — 사용자는
     * "전달 조건" 이 무엇인지, 자기가 무엇을 해야 하는지 알 수 없었다 (#614).
     */
    data class Error(
        val message: UiText,
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
