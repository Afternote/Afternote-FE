package com.afternote.core.network.dto

import kotlinx.serialization.Serializable

/**
 * GET /api/home/summary 응답 data.
 */
@Serializable
data class HomeSummaryResponse(
    val userName: String,
    val isRecipientDesignated: Boolean,
    val diaryCategoryCount: Int,
    val deepThoughtCategoryCount: Int,
)
