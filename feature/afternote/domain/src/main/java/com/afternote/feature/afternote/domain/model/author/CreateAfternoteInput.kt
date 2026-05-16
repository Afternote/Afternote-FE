package com.afternote.feature.afternote.domain.model.author

sealed interface CreateAfternoteInput {
    data class Social(
        val payload: CreateSocialPayload,
    ) : CreateAfternoteInput

    data class Gallery(
        val payload: CreateGalleryPayload,
    ) : CreateAfternoteInput

    data class Playlist(
        val payload: CreatePlaylistPayload,
    ) : CreateAfternoteInput
}
