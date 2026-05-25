package com.afternote.feature.mindrecord.domain.model

data class DeepThought(
    val deepThoughtId: Long,
    val title: String,
    val content: String,
    val category: String,
    val isDraft: Boolean,
    val createdAt: String,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val updatedAt: String? = null,
)

data class DeepThoughtTagCount(
    val tag: String,
    val count: Int,
)

data class DeepThoughtList(
    val items: List<DeepThought>,
    val tagCounts: List<DeepThoughtTagCount>,
)

data class RandomDeepThought(
    val title: String,
    val createdAt: String,
)

data class DeepThoughtCreatePayload(
    val title: String,
    val content: String,
    val isDraft: Boolean,
    val category: String,
    val tags: List<String>? = null,
    val imageUrl: String? = null,
)

data class DeepThoughtUpdatePayload(
    val title: String? = null,
    val content: String? = null,
    val isDraft: Boolean? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val imageUrl: String? = null,
)

data class DeepThoughtCategory(
    val categoryId: Long,
    val title: String,
)

data class DeepThoughtCategoryCreatePayload(
    val category: String,
    val deepThoughtId: Long? = null,
)

data class DeepThoughtCategoryUpdatePayload(
    val category: String,
)
