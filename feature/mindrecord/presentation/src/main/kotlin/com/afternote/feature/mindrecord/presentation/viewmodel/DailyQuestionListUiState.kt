package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.model.DailyQuestion

sealed interface DailyQuestionListUiState {
    data object Loading : DailyQuestionListUiState

    data class Success(
        val todayQuestion: TodayQuestionUi?,
        val answers: List<DailyQuestion>,
        /** 삭제 실패 안내. 조용히 무시하면 항목이 남은 이유를 알 수 없다 (#716). */
        val deleteError: UiText? = null,
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
