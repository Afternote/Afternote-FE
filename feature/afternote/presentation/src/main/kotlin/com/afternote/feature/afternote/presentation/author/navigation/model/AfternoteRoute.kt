package com.afternote.feature.afternote.presentation.author.navigation.model

import com.afternote.feature.afternote.domain.AfternoteType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface AfternoteRoute {
    @Serializable
    data object AfternoteHomeRoute : AfternoteRoute

    @Serializable
    data class DetailRoute(
        val itemId: Long,
    ) : AfternoteRoute

    /** Editor·MemorialPlaylist·AddSong이 공유하는 flow 범위와 생성/수정 인자. */
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
    data class MemorialDetailRoute(
        val itemId: String = "",
    ) : AfternoteRoute
}
