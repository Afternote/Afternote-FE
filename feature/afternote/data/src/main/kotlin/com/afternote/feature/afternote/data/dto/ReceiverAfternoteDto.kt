package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReceiverAfternoteListResponse(
    @SerialName("content") val content: List<ReceiverAfternoteListItem> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("size") val size: Int = 10,
    @SerialName("hasNext") val hasNext: Boolean = false,
)

@Serializable
data class ReceiverAfternoteListItem(
    @SerialName("afternoteId") val afternoteId: Long,
    @SerialName("title") val title: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)
