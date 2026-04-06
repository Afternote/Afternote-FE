package com.afternote.feature.afternote.presentation.author.navigation.model

import kotlinx.serialization.Serializable

sealed interface AfternoteRoute {
    @Serializable
    data object AfternoteHomeRoute : AfternoteRoute

    @Serializable
    data class DetailRoute(
        val itemId: String = "",
    ) : AfternoteRoute

    @Serializable
    data class GalleryDetailRoute(
        val itemId: String = "",
    ) : AfternoteRoute

    @Serializable
    data class EditorRoute(
        val itemId: String? = null,
        val initialCategory: String? = null,
    ) : AfternoteRoute

    @Serializable
    data object FingerprintLoginRoute : AfternoteRoute

    @Serializable
    data object AddSongRoute : AfternoteRoute

    @Serializable
    data object MemorialPlaylistRoute : AfternoteRoute

    @Serializable
    data class MemorialGuidelineDetailRoute(
        val itemId: String = "",
    ) : AfternoteRoute
}
