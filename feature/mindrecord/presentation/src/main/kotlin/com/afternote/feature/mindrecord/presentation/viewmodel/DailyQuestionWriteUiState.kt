package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.model.user.Receiver
import com.afternote.core.ui.UiText

data class DailyQuestionWriteUiState(
    val questionId: Long? = null,
    val questionContent: String = "",
    // 오늘의 질문이 며칠째인지 ("Day N"). 서버가 안 내려주면 null 로 두고 UI 에서 숨긴다.
    val questionDay: Int? = null,
    val answer: String = "",
    val imageUrl: String? = null,
    val receivers: List<Receiver> = emptyList(),
    val selectedReceiverIds: Set<Long> = emptySet(),
    val isQuestionLoading: Boolean = true,
    val questionLoadError: UiText? = null,
    val submitState: SubmitState = SubmitState.Idle,
) {
    val canSubmit: Boolean
        get() = questionId != null && answer.isNotBlank() && submitState != SubmitState.InProgress

    val selectedReceiverNames: List<String>
        get() = receivers.filter { it.receiverId in selectedReceiverIds }.map { it.name }
}

sealed interface SubmitState {
    data object Idle : SubmitState

    data object InProgress : SubmitState

    data object Succeeded : SubmitState

    data class Failed(
        val message: UiText,
    ) : SubmitState
}
