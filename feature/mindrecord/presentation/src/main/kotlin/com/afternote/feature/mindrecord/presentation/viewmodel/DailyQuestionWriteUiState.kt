package com.afternote.feature.mindrecord.presentation.viewmodel

data class DailyQuestionWriteUiState(
    val questionId: Long? = null,
    val questionContent: String = "",
    val answer: String = "",
    val imageUrl: String? = null,
    val isQuestionLoading: Boolean = true,
    val questionLoadError: String? = null,
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
        val message: String,
    ) : SubmitState
}
