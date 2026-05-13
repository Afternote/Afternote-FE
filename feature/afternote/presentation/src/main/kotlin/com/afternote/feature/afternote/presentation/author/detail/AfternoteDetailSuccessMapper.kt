package com.afternote.feature.afternote.presentation.author.detail

import com.afternote.core.model.AlbumCover
import com.afternote.feature.afternote.domain.AfternoteServiceType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.presentation.author.detail.socialnetwork.SocialNetworkDetailContent
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel

/** 상세 화면에 쓰는 "최종 작성일": 갱신일이 있으면 그것, 공백이면 생성일. */
private val Detail.finalWriteDate: String
    get() = timestamps.updatedAt.ifBlank { timestamps.createdAt }

/**
 * 서버가 내려주는 `processMethod` enum 문자열을 사용자 표시 라벨로 변환한다.
 * 매핑되지 않은 값은 raw 문자열을 그대로 반환해 호환성을 유지한다.
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
        finalWriteDate = finalWriteDate,
        afternoteEditReceivers = toReceiverUiModels(),
        processingMethodTitle = processing?.method?.let(::mapProcessMethodLabel) ?: "",
        processingMethods = processing?.actions ?: emptyList(),
        message = processing?.leaveMessage ?: "",
    )

internal fun Detail.toSocialNetworkDetailContent(authorDisplayName: String): SocialNetworkDetailContent =
    SocialNetworkDetailContent(
        serviceName = title,
        userName = authorDisplayName,
        accountId = credentials?.id ?: "",
        password = credentials?.password ?: "",
        accountProcessingMethod = processing?.method?.let(::mapProcessMethodLabel) ?: "",
        processingMethods = processing?.actions ?: emptyList(),
        message = processing?.leaveMessage ?: "",
        finalWriteDate = finalWriteDate,
        afternoteEditReceivers = toReceiverUiModels(),
    )

internal fun Detail.toMemorialGuidelineDetailContent(authorDisplayName: String): MemorialGuidelineDetailContent =
    MemorialGuidelineDetailContent(
        userName = authorDisplayName,
        finalWriteDate = finalWriteDate,
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
