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

@Serializable
data class RandomDeepThoughtResponse(
    @SerialName("title") val title: String,
    @SerialName("createdAt") val createdAt: String,
)
