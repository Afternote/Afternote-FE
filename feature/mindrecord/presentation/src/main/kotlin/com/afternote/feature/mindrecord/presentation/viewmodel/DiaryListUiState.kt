package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.TodayMood
import com.afternote.feature.mindrecord.presentation.model.DailyDiary
import java.time.YearMonth

sealed interface DiaryListUiState {
    data object Loading : DiaryListUiState

    data class Success(
        val diaries: List<DailyDiary>,
        // 기본값을 두지 않는다 — 빠뜨리면 조용히 이번 달로 돌아간다.
        val yearMonth: YearMonth,
        val monthDiaryCount: Int = 0,
        val weeklyDominantMood: TodayMood? = null,
    ) : DiaryListUiState

    data class Error(
        val message: UiText,
    ) : DiaryListUiState
}
