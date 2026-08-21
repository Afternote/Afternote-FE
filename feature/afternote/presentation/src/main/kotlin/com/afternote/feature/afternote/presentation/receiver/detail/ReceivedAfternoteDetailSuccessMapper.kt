package com.afternote.feature.afternote.presentation.receiver.detail

import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.presentation.shared.model.toMessageBlockUiModels

internal fun ReceivedAfternoteDetail.toReceivedDetailContentUiModel(): ReceivedDetailContentUiModel =
    when (type) {
        AfternoteType.SOCIAL_NETWORK -> {
            ReceivedDetailContentUiModel.SocialNetwork(toReceivedSocialNetworkDetailContent())
        }

        AfternoteType.GALLERY_AND_FILES -> {
            ReceivedDetailContentUiModel.Gallery(toReceivedGalleryDetailContent())
        }

        AfternoteType.MEMORIAL -> {
            ReceivedDetailContentUiModel.Memorial(toReceivedMemorialDetailContent())
        }

        // BUSINESS · ESTATE 는 디자인 확정 전 placeholder. 서버도 미지원이라 일반적으로 도달하지 않음.
        AfternoteType.BUSINESS, AfternoteType.ESTATE -> {
            ReceivedDetailContentUiModel.Unimplemented
        }
    }

private fun ReceivedAfternoteDetail.toReceivedSocialNetworkDetailContent(): ReceivedSocialNetworkDetailContent =
    ReceivedSocialNetworkDetailContent(
        serviceName = serviceName.orEmpty(),
        accountId = credentials?.id.orEmpty(),
        password = credentials?.password.orEmpty(),
        processingMethods = processingMethods,
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
        finalWriteDate = createdAt.orEmpty(),
    )

private fun ReceivedAfternoteDetail.toReceivedGalleryDetailContent(): ReceivedGalleryDetailContent =
    ReceivedGalleryDetailContent(
        serviceName = serviceName.orEmpty(),
        finalWriteDate = createdAt.orEmpty(),
        processingMethods = processingMethods,
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
    )

private fun ReceivedAfternoteDetail.toReceivedMemorialDetailContent(): ReceivedMemorialDetailContent {
    val songs = playlist?.songs.orEmpty()
    return ReceivedMemorialDetailContent(
        senderName = senderName.orEmpty(),
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
        albumCovers =
            songs.map { song ->
                AlbumCover(
                    imageUrl = song.coverUrl,
                    title = song.title,
                )
            },
        songCount = songs.size,
        memorialVideoUrl = playlist?.memorialVideoUrl,
        memorialThumbnailUrl = playlist?.memorialThumbnailUrl,
    )
}
