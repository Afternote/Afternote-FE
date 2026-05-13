package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.domain.model.TodayMood
import java.time.LocalDate

data class DiaryWriteUiState(
    val title: String = "",
    val content: String = "",
    val mood: TodayMood = TodayMood.SOSO,
    val date: LocalDate = LocalDate.now(),
    val imageUrl: String? = null,
    val submitState: SubmitState = SubmitState.Idle,
) {
    val canSubmit: Boolean
        get() = title.isNotBlank() && content.isNotBlank() && submitState != SubmitState.InProgress
}
