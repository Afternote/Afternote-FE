package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.component.EmotionBubble
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion
import com.afternote.feature.mindrecord.presentation.model.DayItem
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
        val emotionBubbles: List<EmotionBubble>,
        val summaryText: String,
        val dailyQuestions: List<DailyQuestion>,
    ) : WeeklyReportUiState

    data class Error(
        val message: UiText,
    ) : WeeklyReportUiState
}
