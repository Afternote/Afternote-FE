package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.model.user.Receiver
import com.afternote.feature.mindrecord.domain.model.TodayMood
import java.time.LocalDate

data class DiaryWriteUiState(
    val title: String = "",
    val content: String = "",
    val mood: TodayMood? = null,
    val date: LocalDate = LocalDate.now(),
    val imageUrl: String? = null,
    val receivers: List<Receiver> = emptyList(),
    val selectedReceiverIds: Set<Long> = emptySet(),
    val submitState: SubmitState = SubmitState.Idle,
) {
    val canSubmit: Boolean
        get() = title.isNotBlank() && content.isNotBlank() && mood != null && submitState != SubmitState.InProgress

    val selectedReceiverNames: List<String>
        get() = receivers.filter { it.receiverId in selectedReceiverIds }.map { it.name }
}
