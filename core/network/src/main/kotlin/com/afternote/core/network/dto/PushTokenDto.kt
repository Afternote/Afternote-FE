package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========================================
// Request
// ========================================

/**
 * `PUT users/push-tokens` 요청 (#1493).
 *
 * 서버는 같은 token 을 upsert 로 처리하므로 재호출이 안전하다. [installationId]·[notificationPermission]
 * 은 서버 계약상 선택 필드지만 앱은 늘 싣는다 (#2321) — 빼면 서버가 이 토큰의 권한을 `UNKNOWN` 으로 둔다.
 */
@Serializable
data class RegisterPushTokenRequestDto(
    @SerialName("token") val token: String,
    @SerialName("platform") val platform: String,
    @SerialName("installationId") val installationId: String,
    @SerialName("notificationPermission") val notificationPermission: String,
)

/** `PUT users/push-installations` 요청 (#2321). 토큰 없이도 설치별 OS 알림 권한을 보고한다. */
@Serializable
data class PushInstallationRequestDto(
    @SerialName("installationId") val installationId: String,
    @SerialName("permission") val permission: String,
)

/** `DELETE users/push-tokens` 요청 — 없는 token 을 보내도 200 이다(서버 멱등). */
@Serializable
data class DeletePushTokenRequestDto(
    @SerialName("token") val token: String,
)

// ========================================
// Response
// ========================================

@Serializable
data class PushInstallationDto(
    @SerialName("installationId") val installationId: String,
    @SerialName("permission") val permission: String,
    @SerialName("showPermissionBanner") val showPermissionBanner: Boolean,
)

@Serializable
data class PushTokenDto(
    @SerialName("token") val token: String,
    @SerialName("platform") val platform: String,
    @SerialName("lastSeenAt") val lastSeenAt: String? = null,
)
