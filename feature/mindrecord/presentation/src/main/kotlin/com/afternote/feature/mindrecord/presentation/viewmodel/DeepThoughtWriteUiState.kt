package com.afternote.feature.mindrecord.presentation.viewmodel

data class DeepThoughtWriteUiState(
    val title: String = "",
    val content: String = "",
    val category: String = "나의 가치관",
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val submitState: SubmitState = SubmitState.Idle,
) {
    val canSubmit: Boolean
        get() =
            title.isNotBlank() &&
                content.isNotBlank() &&
                category.isNotBlank() &&
                submitState != SubmitState.InProgress
}
