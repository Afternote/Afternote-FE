package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.EmotionAnalysisStatus
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.DayItem
import com.afternote.feature.mindrecord.presentation.model.EmotionKeyword
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import java.time.LocalDate

/**
 * 드롭다운 표시용. 라벨 문자열은 [monday] 로부터 UI 측에서 포맷팅한다 (월/주차).
 */
data class WeekOption(
    val monday: LocalDate,
)

sealed interface WeeklyReportUiState {
    data object Loading : WeeklyReportUiState

    data class Success(
        val selectedMonday: LocalDate,
        val weekOptions: List<WeekOption>,
        val dateRange: String,
        val userName: String,
        val recordedDays: Int,
        val counts: List<Pair<Int, MindRecordCategoryUi>>,
        val weekDays: List<DayItem>,
        val emotionKeywords: List<EmotionKeyword>,
        /**
         * 감정 분석 진행 상태. [emotionKeywords] 가 비었을 때 그것이 "실제로 0건" 인지
         * "아직 분석 중" 인지 "분석 실패" 인지를 이 값만 가르므로, 빈 목록만 보고 0건을
         * 확정하면 안 된다 (#725).
         */
        val emotionAnalysisStatus: EmotionAnalysisStatus,
        /**
         * 주간 요약. 분석이 끝나기 전에는 서버가 일반 격려 문구를 내려주므로,
         * [emotionAnalysisStatus] 가 완료가 아닐 때 개인화된 요약처럼 보여주면 안 된다.
         */
        val summaryText: String,
        val dailyQuestions: List<DailyQuestion>,
    ) : WeeklyReportUiState

    data class Error(
        val message: UiText,
    ) : WeeklyReportUiState
}
