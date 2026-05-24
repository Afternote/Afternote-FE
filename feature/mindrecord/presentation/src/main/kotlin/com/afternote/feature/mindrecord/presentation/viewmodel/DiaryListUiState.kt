package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.model.DailyDiary

sealed interface DiaryListUiState {
    data object Loading : DiaryListUiState

    data class Success(
        val diaries: List<DailyDiary>,
    ) : DiaryListUiState

    data class Error(
        val message: UiText,
    ) : DiaryListUiState
}
