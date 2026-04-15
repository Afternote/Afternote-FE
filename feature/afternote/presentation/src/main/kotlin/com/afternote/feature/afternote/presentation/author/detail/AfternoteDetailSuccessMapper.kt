package com.afternote.feature.afternote.presentation.author.detail

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.presentation.author.detail.socialnetwork.SocialNetworkDetailContent
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

internal fun Detail.toReceiverUiModels(): List<ReceiverUiModel> =
    receivers.mapIndexed { index, r ->
        ReceiverUiModel(
            id = r.receiverId?.toString() ?: "receiver_$index",
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
        processingMethodTitle = processing?.method ?: "",
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

/**
 * 상세 타입별로 배타적인 UI 모델. Nullable Triple 대신 exhaustive `when` 으로 소비한다.
 *
 * [AfternoteDetailUiState.Success] 에서 참조되므로 모듈 공개(public)로 둔다.
 */
sealed interface DetailContentUiModel {
    data class Gallery(
        val content: GalleryDetailContent,
    ) : DetailContentUiModel

    data class SocialNetwork(
        val content: SocialNetworkDetailContent,
    ) : DetailContentUiModel

    data class Memorial(
        val content: MemorialGuidelineDetailContent,
    ) : DetailContentUiModel
}

internal fun Detail.toDetailContentUiModel(authorDisplayName: String): DetailContentUiModel =
    when (type) {
        AfternoteServiceType.GALLERY_AND_FILES -> {
            DetailContentUiModel.Gallery(toGalleryDetailContent(authorDisplayName))
        }

        AfternoteServiceType.SOCIAL_NETWORK -> {
            DetailContentUiModel.SocialNetwork(toSocialNetworkDetailContent(authorDisplayName))
        }

        AfternoteServiceType.MEMORIAL -> {
            DetailContentUiModel.Memorial(toMemorialGuidelineDetailContent(authorDisplayName))
        }
    }
