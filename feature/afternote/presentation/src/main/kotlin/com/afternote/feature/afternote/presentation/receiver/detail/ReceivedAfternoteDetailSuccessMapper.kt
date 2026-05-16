package com.afternote.feature.afternote.presentation.receiver.detail

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.presentation.shared.model.mapProcessMethodLabel

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

        null -> {
            ReceivedDetailContentUiModel.Unknown
        }
    }

private fun ReceivedAfternoteDetail.toReceivedSocialNetworkDetailContent(): ReceivedSocialNetworkDetailContent =
    ReceivedSocialNetworkDetailContent(
        serviceName = title.orEmpty(),
        accountId = credentials?.id.orEmpty(),
        password = credentials?.password.orEmpty(),
        accountProcessingMethod = processMethod?.let(::mapProcessMethodLabel).orEmpty(),
        processingMethods = actions,
        message = leaveMessage.orEmpty(),
        finalWriteDate = createdAt.orEmpty(),
    )

private fun ReceivedAfternoteDetail.toReceivedGalleryDetailContent(): ReceivedGalleryDetailContent =
    ReceivedGalleryDetailContent(
        serviceName = title.orEmpty(),
        finalWriteDate = createdAt.orEmpty(),
        processingMethodTitle = processMethod?.let(::mapProcessMethodLabel).orEmpty(),
        processingMethods = actions,
        message = leaveMessage.orEmpty(),
    )
