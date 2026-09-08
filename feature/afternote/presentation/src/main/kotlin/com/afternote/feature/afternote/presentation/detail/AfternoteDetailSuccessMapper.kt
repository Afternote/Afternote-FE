package com.afternote.feature.afternote.presentation.detail

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.presentation.detail.account.AccountDetailContent
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel
import com.afternote.feature.afternote.presentation.shared.model.toMessageBlockUiModels

/** 상세 화면에 쓰는 "최종 작성일": 갱신일이 있으면 그것, 공백이면 생성일. */
private val Detail.finalWriteDate: String
    get() = timestamps.updatedAt

internal fun Detail.toReceiverUiModels(): List<ReceiverUiModel> =
    receivers.map { r ->
        ReceiverUiModel(
            id = r.receiverId.toString(),
            name = r.name,
            label = r.relation,
        )
    }

internal fun Detail.toGalleryDetailContent(content: DetailContent.Gallery): GalleryDetailContent =
    GalleryDetailContent(
        serviceName = serviceName,
        finalWriteDate = finalWriteDate,
        afternoteEditReceivers = toReceiverUiModels(),
        processingMethods = content.processingMethods,
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
    )

internal fun Detail.toAccountDetailContent(
    type: AfternoteType,
    credentials: DetailCredentials,
    processingMethods: List<String>,
): AccountDetailContent =
    AccountDetailContent(
        serviceName = serviceName,
        type = type,
        accountId = credentials.id,
        password = credentials.password,
        processingMethods = processingMethods,
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
        finalWriteDate = finalWriteDate,
        afternoteEditReceivers = toReceiverUiModels(),
    )

internal fun Detail.toMemorialDetailContent(content: DetailContent.Memorial): MemorialDetailContent =
    MemorialDetailContent(
        finalWriteDate = finalWriteDate,
        profileImageUri = content.memorial.media.photoUrl,
        afternoteEditReceivers = toReceiverUiModels(),
        albumCovers =
            content.memorial.songs.map { s ->
                AlbumCover(
                    imageUrl = s.coverUrl,
                    title = s.title,
                )
            },
        songCount = content.memorial.songs.size,
        memorialVideoUrl = content.memorial.media.videoUrl,
        memorialThumbnailUrl = content.memorial.media.thumbnailUrl,
    )

/**
 * 상세 타입별로 배타적인 UI 모델. Nullable Triple 대신 exhaustive `when` 으로 소비한다.
 *
 * [AfternoteDetailUiState.Success] 에서 참조되므로 모듈 공개(public)로 둔다.
 */
sealed interface DetailContentUiModel {
    val type: AfternoteType

    data class Gallery(
        val content: GalleryDetailContent,
    ) : DetailContentUiModel {
        override val type: AfternoteType = AfternoteType.GALLERY_AND_FILES
    }

    data class SocialNetwork(
        val content: AccountDetailContent,
    ) : DetailContentUiModel {
        override val type: AfternoteType = AfternoteType.SOCIAL_NETWORK
    }

    data class Business(
        val content: AccountDetailContent,
    ) : DetailContentUiModel {
        override val type: AfternoteType = AfternoteType.BUSINESS
    }

    data class Memorial(
        val content: MemorialDetailContent,
    ) : DetailContentUiModel {
        override val type: AfternoteType = AfternoteType.MEMORIAL
    }

    /** ESTATE 등 디자인 확정 전 placeholder. */
    data object Unimplemented : DetailContentUiModel {
        override val type: AfternoteType = AfternoteType.ESTATE
    }
}

internal fun Detail.toDetailContentUiModel(): DetailContentUiModel =
    when (val content = content) {
        is DetailContent.Gallery -> {
            DetailContentUiModel.Gallery(toGalleryDetailContent(content))
        }

        is DetailContent.SocialNetwork -> {
            DetailContentUiModel.SocialNetwork(
                toAccountDetailContent(
                    type = content.type,
                    credentials = content.credentials,
                    processingMethods = content.processingMethods,
                ),
            )
        }

        // BUSINESS 는 타입을 구분하되 현재 화면 구성이 SOCIAL 과 같아 표시 데이터와 화면 컴포넌트만 공유한다 (이슈 #467).
        is DetailContent.Business -> {
            DetailContentUiModel.Business(
                toAccountDetailContent(
                    type = content.type,
                    credentials = content.credentials,
                    processingMethods = content.processingMethods,
                ),
            )
        }

        is DetailContent.Memorial -> {
            DetailContentUiModel.Memorial(toMemorialDetailContent(content))
        }

        // ESTATE 는 디자인 확정 전 placeholder. 백엔드도 미지원이라 일반적으로 도달하지 않음.
        DetailContent.Estate -> {
            DetailContentUiModel.Unimplemented
        }
    }
