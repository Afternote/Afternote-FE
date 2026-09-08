package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========================================
// Request
// ========================================

/**
 * `PUT users/push-tokens` 요청 (#1493).
 *
 * 서버는 같은 token 을 upsert 로 처리하므로 재호출이 안전하다.
 */
@Serializable
data class RegisterPushTokenRequestDto(
    @SerialName("token") val token: String,
    @SerialName("platform") val platform: String,
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
data class PushTokenDto(
    @SerialName("token") val token: String,
    @SerialName("platform") val platform: String,
    @SerialName("lastSeenAt") val lastSeenAt: String? = null,
)
