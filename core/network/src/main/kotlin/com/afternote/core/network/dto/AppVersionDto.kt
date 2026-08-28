package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 앱 버전 API가 현재 지원하는 플랫폼 값. */
@Serializable
enum class AppPlatformDto {
    @SerialName("ANDROID")
    ANDROID,
}

/**
 * 앱 버전 확인 결과.
 *
 * 강제 업데이트 판단값이 누락됐을 때 조용히 "업데이트 불필요"로 처리되지 않도록 서버 필수 필드에는
 * 기본값을 두지 않는다. [storeUrl]은 업데이트가 필요하지 않으면 명시적인 null이다.
 */
@Serializable
data class AppVersionDto(
    @SerialName("updateRequired") val updateRequired: Boolean,
    @SerialName("latestVersionCode") val latestVersionCode: Int,
    @SerialName("storeUrl") val storeUrl: String?,
)
