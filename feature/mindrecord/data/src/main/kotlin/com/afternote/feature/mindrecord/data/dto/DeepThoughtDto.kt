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
    @SerialName("is_draft") val isDraft: Boolean,
    @SerialName("tag") val tag: List<String> = emptyList(),
    @SerialName("imageUrl") val imageUrl: String? = null,
)

// `/deep-thought` 응답의 `data` 는 배열이 아니라 객체 — `deepThoughts` 외에도
// `tagCounts` 가 함께 내려오지만 현재는 목록만 사용.
// (Json 글로벌 설정에 `ignoreUnknownKeys = true` 적용됨)
@Serializable
data class DeepThoughtListResponse(
    @SerialName("deepThoughts") val deepThoughts: List<DeepThoughtListItem> = emptyList(),
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
