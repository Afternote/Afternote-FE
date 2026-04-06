package com.afternote.feature.afternote.domain.model.input

// AfternoteRepository.getListPage() 인자
data class GetListPageInput(
    val category: String?,
    val pageNumber: Int,
    val size: Int,
)
