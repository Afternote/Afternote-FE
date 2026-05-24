package com.afternote.feature.afternote.presentation.receiver.detail

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail

internal fun ReceivedAfternoteDetail.toReceivedDetailContentUiModel(): ReceivedDetailContentUiModel =
    when (type) {
        AfternoteServiceType.SOCIAL_NETWORK -> {
            ReceivedDetailContentUiModel.SocialNetwork(toReceivedSocialNetworkDetailContent())
        }

        AfternoteServiceType.GALLERY_AND_FILES -> {
            ReceivedDetailContentUiModel.Gallery(toReceivedGalleryDetailContent())
        }

        AfternoteServiceType.MEMORIAL -> {
            ReceivedDetailContentUiModel.MemorialPending
        }

        // BUSINESS · ESTATE 는 디자인 확정 전 placeholder. 서버도 미지원이라 일반적으로 도달하지 않음.
        AfternoteServiceType.BUSINESS, AfternoteServiceType.ESTATE -> {
            ReceivedDetailContentUiModel.Unimplemented
        }

        null -> {
            ReceivedDetailContentUiModel.Unknown
        }
    }

private fun ReceivedAfternoteDetail.toReceivedSocialNetworkDetailContent(): ReceivedSocialNetworkDetailContent =
    ReceivedSocialNetworkDetailContent(
        serviceName = title.orEmpty(),
        accountId = credentials?.id.orEmpty(),
        password = credentials?.password.orEmpty(),
        processingMethods = actions,
        message = leaveMessage.orEmpty(),
        finalWriteDate = createdAt.orEmpty(),
    )

private fun ReceivedAfternoteDetail.toReceivedGalleryDetailContent(): ReceivedGalleryDetailContent =
    ReceivedGalleryDetailContent(
        serviceName = title.orEmpty(),
        finalWriteDate = createdAt.orEmpty(),
        processingMethods = actions,
        message = leaveMessage.orEmpty(),
    )
