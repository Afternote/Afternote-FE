package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepThoughtCreateRequest(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("category") val category: String,
    @SerialName("tag") val tag: List<String>? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DeepThoughtUpdateRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("isDraft") val isDraft: Boolean? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("tag") val tag: List<String>? = null,
    @SerialName("imageUrl") val imageUrl: String? = null,
)

@Serializable
data class DeepThoughtListItem(
    @SerialName("deepThoughtId") val deepThoughtId: Long,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("category") val category: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
data class DeepThoughtTagCountDto(
    @SerialName("tag") val tag: String,
    @SerialName("count") val count: Int,
)

@Serializable
data class DeepThoughtListResponse(
    @SerialName("deepThoughts") val deepThoughts: List<DeepThoughtListItem> = emptyList(),
    @SerialName("tagCounts") val tagCounts: List<DeepThoughtTagCountDto> = emptyList(),
)

@Serializable
data class RandomDeepThoughtResponse(
    @SerialName("title") val title: String,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class DeepThoughtCategoryItem(
    @SerialName("categoryId") val categoryId: Long,
    @SerialName("title") val title: String,
)

@Serializable
data class DeepThoughtCategoryCreateRequest(
    @SerialName("deepThoughtId") val deepThoughtId: Long? = null,
    @SerialName("category") val category: String,
)

@Serializable
data class DeepThoughtCategoryUpdateRequest(
    @SerialName("category") val category: String,
)

@Serializable
data class DeepThoughtCategoryMutationResponse(
    @SerialName("categoryId") val categoryId: Long,
    @SerialName("title") val title: String,
)
