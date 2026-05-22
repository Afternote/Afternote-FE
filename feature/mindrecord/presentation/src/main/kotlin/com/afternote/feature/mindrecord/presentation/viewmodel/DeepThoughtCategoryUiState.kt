package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.presentation.model.CategoryUiModel

data class DeepThoughtCategoryUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val categories: List<CategoryUiModel> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface DeepThoughtCategoryEvent {
    data class Error(
        val message: String,
    ) : DeepThoughtCategoryEvent

    data class Created(
        val category: CategoryUiModel,
    ) : DeepThoughtCategoryEvent

    data class Updated(
        val category: CategoryUiModel,
    ) : DeepThoughtCategoryEvent

    data class Deleted(
        val categoryId: Long,
    ) : DeepThoughtCategoryEvent
}
