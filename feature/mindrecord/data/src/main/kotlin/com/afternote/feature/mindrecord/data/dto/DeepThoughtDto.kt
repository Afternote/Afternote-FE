package com.afternote.feature.mindrecord.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /deep-thought` 응답 (`data`). 목록 외에 태그 집계도 함께 오지만 여기선 쓰지 않는다. */
@Serializable
data class DeepThoughtListDto(
    @SerialName("deepThoughts") val deepThoughts: List<DeepThoughtItemDto> = emptyList(),
)

@Serializable
data class DeepThoughtItemDto(
    @SerialName("deepThoughtId") val deepThoughtId: Long,
    @SerialName("title") val title: String,
    // "yyyy.MM.dd 요일" 형식.
    @SerialName("createdAt") val createdAt: String,
)
