package com.afternote.feature.mindrecord.presentation.viewmodel

import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.presentation.model.CategoryUiModel
import com.afternote.feature.mindrecord.presentation.model.DeepThoughtModel
import com.afternote.feature.mindrecord.presentation.model.Tag

sealed interface DeepThoughtListUiState {
    data object Loading : DeepThoughtListUiState

    data class Success(
        val randomThoughtTitle: String?,
        val randomThoughtCreatedAt: String?,
        val tags: List<Tag>,
        val selectedTag: Tag?,
        val categories: List<CategoryUiModel>,
        val selectedCategory: String?,
        val items: List<DeepThoughtModel>,
    ) : DeepThoughtListUiState

    data class Error(
        val message: UiText,
    ) : DeepThoughtListUiState
}
