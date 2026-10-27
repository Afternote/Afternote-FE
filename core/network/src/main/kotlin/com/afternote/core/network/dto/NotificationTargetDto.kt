package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET users/notifications/{id}/target` 응답 (#2321).
 *
 * [data] 는 FCM data 와 같은 문자열 맵이다 — `destination` 과 그 목적지의 ID·`action` 이 들어 있다.
 */
@Serializable
data class NotificationTargetDto(
    @SerialName("data") val data: Map<String, String>,
    @SerialName("deleted") val deleted: Boolean,
    @SerialName("toast") val toast: String? = null,
    @SerialName("backDestination") val backDestination: String? = null,
)
