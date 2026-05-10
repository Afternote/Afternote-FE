package com.afternote.feature.afternote.presentation.receiver.detail

import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod

/**
 * 서버 `processMethod` enum 문자열 → 사용자 표시 라벨.
 *
 * 발신자 상세 매퍼([com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailSuccessMapper])
 * 와 동일한 규칙. 향후 공용 매퍼로 추출 검토.
 */
private fun mapProcessMethodLabel(serverValue: String): String =
    when (serverValue) {
        "MEMORIAL" -> AccountProcessingMethod.MEMORIAL_ACCOUNT.title
        "TRANSFER" -> AccountProcessingMethod.TRANSFER_TO_RECEIVER.title
        "PERMANENT_DELETE" -> AccountProcessingMethod.PERMANENT_DELETE.title
        "TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER" -> InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER.title
        "TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER" -> InformationProcessingMethod.TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER.title
        else -> serverValue
    }

internal fun ReceivedAfternoteDetail.toReceivedDetailContentUiModel(): ReceivedDetailContentUiModel =
    when (type) {
        AfternoteServiceType.SOCIAL_NETWORK ->
            ReceivedDetailContentUiModel.SocialNetwork(toReceivedSocialNetworkDetailContent())

        AfternoteServiceType.GALLERY_AND_FILES ->
            ReceivedDetailContentUiModel.Gallery(toReceivedGalleryDetailContent())

        AfternoteServiceType.MEMORIAL -> ReceivedDetailContentUiModel.MemorialPending
        null -> ReceivedDetailContentUiModel.Unknown
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
