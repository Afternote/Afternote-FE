package com.afternote.feature.setting.presentation.model

enum class InquiryStatus { RECEIVED, ANSWERED }

data class InquiryUiModel(
    val id: Long,
    val status: InquiryStatus,
    val date: String,
    val title: String,
    val content: String,
    val answer: String?,
)
