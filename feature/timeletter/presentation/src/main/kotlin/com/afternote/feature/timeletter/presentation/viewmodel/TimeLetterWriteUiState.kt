package com.afternote.feature.timeletter.presentation.viewmodel

data class TimeLetterWriteUiState(
    val recipientName: String = "박채연",
    val sendDate: String = "2026.03.22.",
    val sendTime: String = "09:22",
    val draftCount: Int = 1,
)
