package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// GET /api/v1/app/version — 앱 버전 확인(강제 업데이트 여부)
@Serializable
data class AppVersionCheckResponseDto(
    @SerialName("updateRequired") val updateRequired: Boolean = false,
    @SerialName("latestVersionCode") val latestVersionCode: Int = 0,
    @SerialName("storeUrl") val storeUrl: String? = null,
)
