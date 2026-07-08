package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepThoughtCreateRequest(
    @SerialName("title") val title: String,
    @SerialName("content") val content: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("category") val category: String,
    @SerialName("tags") val tags: List<String>? = null,
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
)

@Serializable
data class DeepThoughtUpdateRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("isDraft") val isDraft: Boolean? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("tags") val tags: List<String>? = null,
    @SerialName("receiverIds") val receiverIds: List<Long> = emptyList(),
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

// 서버 응답 `data` 는 배열이 아니라 `{ "categories": [...] }` 객체로 감싸져 있음.
@Serializable
data class DeepThoughtCategoryListResponse(
    @SerialName("categories") val categories: List<DeepThoughtCategoryItem> = emptyList(),
)

@Serializable
data class DeepThoughtCategoryCreateRequest(
    @SerialName("title") val title: String,
)

@Serializable
data class DeepThoughtCategoryUpdateRequest(
    @SerialName("title") val title: String,
)

@Serializable
data class DeepThoughtCategoryMutationResponse(
    @SerialName("categoryId") val categoryId: Long,
    @SerialName("title") val title: String,
)
