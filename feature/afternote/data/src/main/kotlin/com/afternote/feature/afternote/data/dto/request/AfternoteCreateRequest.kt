package com.afternote.feature.afternote.data.dto.request

import com.afternote.feature.afternote.data.dto.AfternoteCredentials
import com.afternote.feature.afternote.data.dto.AfternotePlaylist
import com.afternote.feature.afternote.data.dto.AfternoteReceiverRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unified server request form for POST /api/afternotes.
 * Covers all categories (SOCIAL, GALLERY, PLAYLIST).
 * Category-specific fields are null when not applicable:
 * - SOCIAL: processMethod, actions, leaveMessage, credentials, receivers
 * - GALLERY: processMethod, actions, leaveMessage, receivers
 * - PLAYLIST: playlist, receivers
 */
@Serializable
data class AfternoteCreateRequest(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("processMethod") val processMethod: String? = null,
    @SerialName("actions") val actions: List<String>? = null,
    @SerialName("leaveMessage") val leaveMessage: String? = null,
    @SerialName("credentials") val credentials: AfternoteCredentials? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRef>? = null,
    @SerialName("playlist") val playlist: AfternotePlaylist? = null,
)
