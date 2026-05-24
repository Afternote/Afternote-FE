package com.afternote.core.model

data class HomeSummary(
    val userName: String,
    val isRecipientDesignated: Boolean,
    val diaryCategoryCount: Int,
    val deepThoughtCategoryCount: Int,
)
