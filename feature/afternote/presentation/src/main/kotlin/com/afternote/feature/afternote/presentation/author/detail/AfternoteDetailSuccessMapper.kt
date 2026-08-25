package com.afternote.feature.afternote.presentation.author.detail

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.presentation.author.detail.account.AccountDetailContent
import com.afternote.feature.afternote.presentation.shared.model.AlbumCover
import com.afternote.feature.afternote.presentation.shared.model.ReceiverUiModel
import com.afternote.feature.afternote.presentation.shared.model.toMessageBlockUiModels

/** 상세 화면에 쓰는 "최종 작성일": 갱신일이 있으면 그것, 공백이면 생성일. */
private val Detail.finalWriteDate: String
    get() = timestamps.updatedAt.ifBlank { timestamps.createdAt }

internal fun Detail.toReceiverUiModels(): List<ReceiverUiModel> =
    receivers.map { r ->
        ReceiverUiModel(
            id = r.receiverId.toString(),
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
        processingMethods = processingMethods,
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
    )

internal fun Detail.toAccountDetailContent(authorDisplayName: String): AccountDetailContent =
    AccountDetailContent(
        serviceName = title,
        type = type,
        userName = authorDisplayName,
        accountId = credentials?.id ?: "",
        password = credentials?.password ?: "",
        processingMethods = processingMethods,
        messageBlocks = leaveMessageBlocks.toMessageBlockUiModels(),
        finalWriteDate = finalWriteDate,
        afternoteEditReceivers = toReceiverUiModels(),
    )

internal fun Detail.toMemorialDetailContent(authorDisplayName: String): MemorialDetailContent =
    MemorialDetailContent(
        userName = authorDisplayName,
        finalWriteDate = finalWriteDate,
        profileImageUri = memorial?.media?.photoUrl,
        afternoteEditReceivers = toReceiverUiModels(),
        albumCovers =
            memorial?.songs?.map { s ->
                AlbumCover(
                    id = (s.id ?: 0L).toString(),
                    imageUrl = s.coverUrl,
                    title = s.title,
                )
            } ?: emptyList(),
        songCount = memorial?.songs?.size ?: 0,
        memorialVideoUrl = memorial?.media?.videoUrl,
        memorialThumbnailUrl = memorial?.media?.thumbnailUrl,
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

    /** 계정 기반 상세(SOCIAL·BUSINESS 공용) — 두 카테고리는 상세 데이터 구성이 동일하다 (이슈 #467). */
    data class Account(
        val content: AccountDetailContent,
    ) : DetailContentUiModel

    data class Memorial(
        val content: MemorialDetailContent,
    ) : DetailContentUiModel

    /** ESTATE 등 디자인 확정 전 placeholder. */
    data object Unimplemented : DetailContentUiModel
}

internal fun Detail.toDetailContentUiModel(authorDisplayName: String): DetailContentUiModel =
    when (type) {
        AfternoteType.GALLERY_AND_FILES -> {
            DetailContentUiModel.Gallery(toGalleryDetailContent(authorDisplayName))
        }

        // BUSINESS 는 데이터 구성이 SOCIAL 과 동일(계정 정보·처리 방법·남긴 말씀)해 계정 상세 UI 모델을 공유한다 (이슈 #467).
        AfternoteType.SOCIAL_NETWORK, AfternoteType.BUSINESS -> {
            DetailContentUiModel.Account(toAccountDetailContent(authorDisplayName))
        }

        AfternoteType.MEMORIAL -> {
            DetailContentUiModel.Memorial(toMemorialDetailContent(authorDisplayName))
        }

        // ESTATE 는 디자인 확정 전 placeholder. 백엔드도 미지원이라 일반적으로 도달하지 않음.
        AfternoteType.ESTATE -> {
            DetailContentUiModel.Unimplemented
        }
    }
