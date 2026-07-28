package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import java.time.YearMonth

sealed interface DiaryListUiState {
    data object Loading : DiaryListUiState

    data class Success(
        val diaries: List<DailyDiary>,
        val yearMonth: YearMonth = YearMonth.now(),
        val monthDiaryCount: Int = 0,
        val weeklyDominantMood: TodayMood? = null,
    ) : DiaryListUiState

    data class Error(
        val message: UiText,
    ) : DiaryListUiState
}
