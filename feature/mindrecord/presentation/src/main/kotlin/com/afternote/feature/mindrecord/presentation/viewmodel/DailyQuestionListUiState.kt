package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion

sealed interface DailyQuestionListUiState {
    data object Loading : DailyQuestionListUiState

    data class Success(
        val todayQuestion: TodayQuestionUi?,
        val answers: List<DailyQuestion>,
    ) : DailyQuestionListUiState

    data class Error(
        val message: UiText,
    ) : DailyQuestionListUiState
}

data class TodayQuestionUi(
    val questionId: Long,
    val content: String,
    val isAnswered: Boolean,
)
