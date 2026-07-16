package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText

data class DailyQuestionWriteUiState(
    val questionId: Long? = null,
    /** "Day N" 배너 표기용 — 오늘의 질문이 서비스 기준 몇 일차인지. */
    val questionDay: Int? = null,
    val questionContent: String = "",
    val answer: String = "",
    val imageUrl: String? = null,
    val isQuestionLoading: Boolean = true,
    val questionLoadError: UiText? = null,
    val submitState: SubmitState = SubmitState.Idle,
) {
    val canSubmit: Boolean
        get() = questionId != null && answer.isNotBlank() && submitState != SubmitState.InProgress
}

sealed interface SubmitState {
    data object Idle : SubmitState

    data object InProgress : SubmitState

    data object Succeeded : SubmitState

    data class Failed(
        val message: UiText,
    ) : SubmitState
}
