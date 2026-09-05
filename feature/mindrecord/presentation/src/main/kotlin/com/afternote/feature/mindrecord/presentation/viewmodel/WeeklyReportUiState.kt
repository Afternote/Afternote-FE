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
        /**
         * 주차 조회에 실패했지만 **직전에 보던 리포트가 남아 있는** 상태 (#723).
         *
         * 실패해도 화면 전체를 오류로 바꾸지 않는다. 리포트와 주차 선택 UI 를 유지한 채
         * 배너로만 알려야 사용자가 재시도하거나 다른 주차로 옮길 수 있다.
         */
        val loadFailure: LoadFailure? = null,
    ) : WeeklyReportUiState

    /** 화면을 유지한 채 알리는 조회 실패. [failedWeekLabel] 은 사용자가 고른 주의 월요일. */
    data class LoadFailure(
        val message: UiText,
        val failedWeekLabel: LocalDate,
    )

    /**
     * 보여 줄 리포트가 하나도 없는 실패 (첫 조회부터 실패).
     *
     * 이때도 [weekOptions] 와 [failedMonday] 를 함께 넘긴다 — 재시도와 주차 재선택 수단을
     * 화면에 남겨야 복구가 가능하다. 종전에는 오류 문구 하나만 렌더해 둘 다 사라졌다 (#723).
     */
    data class Error(
        val message: UiText,
        val weekOptions: List<WeekOption>,
        val failedMonday: LocalDate,
    ) : WeeklyReportUiState
}
