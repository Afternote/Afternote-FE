package com.afternote.feature.afternote.domain.model

// AfternoteRepository.getAfternotes() 인자
data class GetAfternotesInput(
    val category: String?,
    val page: Int,
    val size: Int,
)
