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
    /** "Day N" 배너 표기용 — 서비스 시작일 기준 몇 번째 질문인지. */
    val day: Int,
    val content: String,
    val isAnswered: Boolean,
)
