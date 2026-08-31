package com.afternote.feature.afternote.presentation.receiver.detail

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.toMessageBlockUiModels
import com.afternote.feature.receiver.domain.model.ReceivedAccountCredentials
import com.afternote.feature.receiver.domain.model.ReceivedAfternoteDetail

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
        serviceName = serviceName,
        credentials = credentials.toUiModelOrNull(),
        processingMethods = processingMethods,
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
        finalWriteDate = createdAt.orEmpty(),
    )

/**
 * 계정 자격증명을 표시 모델로 옮긴다.
 *
 * 공백뿐인 값은 미제공과 구분할 수 없으므로 `null` 로 접고, 그러고도 남는 값이 없으면 모델 자체를
 * 만들지 않는다. 예전처럼 `orEmpty()` 로 뭉개면 화면이 빈 비밀번호에 마스킹을 그려, 수신자가
 * 가려진 값이 있다고 오인한 채 "표시" 를 눌러도 아무것도 얻지 못했다 (#619).
 */
private fun ReceivedAccountCredentials?.toUiModelOrNull(): ReceivedAccountCredentialsUiModel? {
    val accountId = this?.id?.takeIf(String::isNotBlank)
    val password = this?.password?.takeIf(String::isNotBlank)
    return if (accountId == null && password == null) {
        null
    } else {
        ReceivedAccountCredentialsUiModel(accountId = accountId, password = password)
    }
}

private fun ReceivedAfternoteDetail.toReceivedGalleryDetailContent(): ReceivedGalleryDetailContent =
    ReceivedGalleryDetailContent(
        serviceName = serviceName,
        finalWriteDate = createdAt.orEmpty(),
        processingMethods = processingMethods,
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
    )

private fun ReceivedAfternoteDetail.toReceivedMemorialDetailContent(): ReceivedMemorialDetailContent {
    val songs = playlist?.songs.orEmpty()
    return ReceivedMemorialDetailContent(
        senderName = senderName,
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
