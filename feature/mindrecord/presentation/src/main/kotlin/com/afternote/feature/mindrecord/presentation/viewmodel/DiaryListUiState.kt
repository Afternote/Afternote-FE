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
        /** 삭제 실패 안내. 조용히 무시하면 항목이 남은 이유를 알 수 없다 (#716). */
        val deleteError: UiText? = null,
    ) : DiaryListUiState

    data class Error(
        val message: UiText,
    ) : DiaryListUiState
}
