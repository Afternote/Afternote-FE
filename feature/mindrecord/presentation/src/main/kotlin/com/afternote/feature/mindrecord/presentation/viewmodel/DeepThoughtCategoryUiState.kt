package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.feature.mindrecord.presentation.model.CategoryUiModel

data class DeepThoughtCategoryUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val categories: List<CategoryUiModel> = emptyList(),
    val error: CategoryError? = null,
)
