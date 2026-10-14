package com.afternote.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `POST receiver-invitations` 응답 (#944, BE main 3593aa30 · 2026-09-18 실측).
 *
 * 서버가 주는 필드를 전부 적어 계약을 코드에 고정한다. 도메인으로 올리는 것은 소비처가 있는
 * `invitationToken` 뿐이고, 나머지는 파싱만 한다. `expiresAt` 은 `LocalDateTime` 직렬화라
 * 나노초 자릿수가 가변인 문자열이다 — 이 앱에는 파싱하는 곳이 없어 문자열로 둔다.
 */
@Serializable
data class ReceiverInvitationCreateDto(
    @SerialName("invitationId") val invitationId: Long,
    @SerialName("invitationToken") val invitationToken: String,
    @SerialName("invitationUrl") val invitationUrl: String,
    @SerialName("expiresAt") val expiresAt: String,
)

/** `GET receiver-invitations/{token}` 응답. `status` 는 `PENDING`·`ACCEPTED`. */
@Serializable
data class ReceiverInvitationLookupDto(
    @SerialName("inviterName") val inviterName: String,
    @SerialName("status") val status: String,
    @SerialName("expired") val expired: Boolean,
    @SerialName("expiresAt") val expiresAt: String,
)

/** `POST receiver-invitations/{token}/accept` 응답. */
@Serializable
data class ReceiverInvitationAcceptDto(
    @SerialName("receiverId") val receiverId: Long,
    @SerialName("inviterName") val inviterName: String,
)
