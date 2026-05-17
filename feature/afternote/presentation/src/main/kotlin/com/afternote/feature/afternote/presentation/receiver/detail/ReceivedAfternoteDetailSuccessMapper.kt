package com.afternote.feature.afternote.presentation.receiver.detail

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail

internal fun ReceivedAfternoteDetail.toReceivedDetailContentUiModel(): ReceivedDetailContentUiModel =
    when (type) {
        // BUSINESS 도 Social 상세 UI 재사용 (디자인 확정 후 분리).
        AfternoteServiceType.SOCIAL_NETWORK, AfternoteServiceType.BUSINESS -> {
            ReceivedDetailContentUiModel.SocialNetwork(toReceivedSocialNetworkDetailContent())
        }

        // ESTATE 도 Gallery 상세 UI 재사용 (디자인 확정 후 분리).
        AfternoteServiceType.GALLERY_AND_FILES, AfternoteServiceType.ESTATE -> {
            ReceivedDetailContentUiModel.Gallery(toReceivedGalleryDetailContent())
        }

        AfternoteServiceType.MEMORIAL -> {
            ReceivedDetailContentUiModel.MemorialPending
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
