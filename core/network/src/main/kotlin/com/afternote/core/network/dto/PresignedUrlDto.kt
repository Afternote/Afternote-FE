package com.afternote.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PresignedUrlRequestDto(
    val directory: String,
    val extension: String,
)

@Serializable
data class PresignedUrlDto(
    val presignedUrl: String,
    val fileKey: String,
    val fileUrl: String,
    val contentType: String,
)
