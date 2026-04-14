package com.afternote.feature.afternote.presentation.author.detail

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel
import com.afternote.feature.afternote.presentation.shared.util.getIconResForServiceName

internal fun Detail.toReceiverUiModels(): List<ReceiverUiModel> =
    receivers.map { r ->
        ReceiverUiModel(
            id = "",
            name = r.name,
            label = r.relation,
        )
    }

internal fun Detail.toGalleryDetailContent(authorDisplayName: String): GalleryDetailContent =
    GalleryDetailContent(
        serviceName = title,
        userName = authorDisplayName,
        finalWriteDate = timestamps.updatedAt.ifEmpty { timestamps.createdAt },
        afternoteEditReceivers = toReceiverUiModels(),
        processingMethods = processing?.actions ?: emptyList(),
        message = processing?.leaveMessage ?: "",
    )

internal fun Detail.toSocialNetworkDetailContent(authorDisplayName: String): SocialNetworkDetailContent =
    SocialNetworkDetailContent(
        serviceName = title,
        userName = authorDisplayName,
        accountId = credentials?.id ?: "",
        password = credentials?.password ?: "",
        accountProcessingMethod = processing?.method ?: "",
        processingMethods = processing?.actions ?: emptyList(),
        message = processing?.leaveMessage ?: "",
        finalWriteDate = timestamps.updatedAt.ifEmpty { timestamps.createdAt },
        afternoteEditReceivers = toReceiverUiModels(),
        iconResId = getIconResForServiceName(title),
    )

internal fun Detail.toMemorialGuidelineDetailContent(authorDisplayName: String): MemorialGuidelineDetailContent =
    MemorialGuidelineDetailContent(
        userName = authorDisplayName,
        finalWriteDate = timestamps.updatedAt.ifEmpty { timestamps.createdAt },
        profileImageUri = playlist?.playlistDetailMemorialMedia?.photoUrl,
        afternoteEditReceivers = toReceiverUiModels(),
        albumCovers =
            playlist?.songs?.map { s ->
                AlbumCover(
                    id = (s.id ?: 0L).toString(),
                    imageUrl = s.coverUrl,
                    title = s.title,
                )
            } ?: emptyList(),
        songCount = playlist?.songs?.size ?: 0,
        lastWish = playlist?.atmosphere ?: "",
        memorialVideoUrl = playlist?.playlistDetailMemorialMedia?.videoUrl,
        memorialThumbnailUrl = playlist?.playlistDetailMemorialMedia?.thumbnailUrl,
    )

internal fun Detail.toSuccessUiSlices(
    authorDisplayName: String,
): Triple<GalleryDetailContent?, SocialNetworkDetailContent?, MemorialGuidelineDetailContent?> =
    when (type) {
        AfternoteServiceType.GALLERY_AND_FILES ->
            Triple(
                toGalleryDetailContent(authorDisplayName),
                null,
                null,
            )

        AfternoteServiceType.SOCIAL_NETWORK ->
            Triple(
                null,
                toSocialNetworkDetailContent(authorDisplayName),
                null,
            )

        AfternoteServiceType.MEMORIAL ->
            Triple(
                null,
                null,
                toMemorialGuidelineDetailContent(authorDisplayName),
            )
    }
