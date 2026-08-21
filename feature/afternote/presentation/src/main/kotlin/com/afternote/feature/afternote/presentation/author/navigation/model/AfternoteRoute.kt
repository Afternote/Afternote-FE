package com.afternote.feature.afternote.presentation.author.navigation.model

import com.afternote.feature.afternote.domain.AfternoteType
import kotlinx.serialization.Serializable

sealed interface AfternoteRoute {
    @Serializable
    data object AfternoteHomeRoute : AfternoteRoute

    @Serializable
    data class DetailRoute(
        val itemId: Long,
    ) : AfternoteRoute

    @Serializable
    data class EditorRoute(
        val itemId: Long? = null,
        val initialType: AfternoteType,
    ) : AfternoteRoute

    @Serializable
    data object AddSongRoute : AfternoteRoute

    @Serializable
    data object MemorialPlaylistRoute : AfternoteRoute

    @Serializable
    data object FingerprintLoginRoute : AfternoteRoute
}
