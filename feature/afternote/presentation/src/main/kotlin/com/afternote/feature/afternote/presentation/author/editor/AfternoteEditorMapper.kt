package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.model.Detail
import com.afternote.feature.afternote.domain.model.input.CreateGalleryInput
import com.afternote.feature.afternote.domain.model.input.CreatePlaylistInput
import com.afternote.feature.afternote.domain.model.input.CreateSocialInput
import com.afternote.feature.afternote.domain.model.input.CredentialsInput
import com.afternote.feature.afternote.domain.model.input.MemorialVideoInput
import com.afternote.feature.afternote.domain.model.input.PlaylistInput
import com.afternote.feature.afternote.domain.model.input.ReceiverRefInput
import com.afternote.feature.afternote.domain.model.input.SongInput
import com.afternote.feature.afternote.domain.model.input.UpdateInput
import com.afternote.feature.afternote.presentation.author.editor.model.AccountProcessMethod
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.InfoProcessMethod
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingAccountParams
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingParams
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingProcessingParams
import com.afternote.feature.afternote.presentation.author.editor.model.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem

/**
 * ViewModel ↔ Domain 간 데이터 변환 로직을 담당합니다.
 */
internal object AfternoteEditorMapper {
    fun buildLoadFromExistingParams(detail: Detail): LoadFromExistingParams {
        val actionItems =
            detail.processing?.actions?.mapIndexed { index, text ->
                ProcessingMethodItem(
                    id = (index + 1).toString(),
                    text = text,
                )
            } ?: emptyList()
        val processMethod = detail.processing?.method ?: ""
        val editorCategory = EditorCategory.fromServerValue(detail.category)
        val isGallery = editorCategory == EditorCategory.GALLERY
        val isSocial = editorCategory == EditorCategory.SOCIAL
        val accountProcessingMethodName =
            if (isSocial) {
                AccountProcessMethod.fromServerValue(processMethod)?.clientName ?: processMethod
            } else {
                ""
            }
        val informationProcessingMethodName =
            if (isGallery) {
                InfoProcessMethod.fromServerValue(processMethod)?.clientName ?: processMethod
            } else {
                ""
            }
        return LoadFromExistingParams(
            itemId = detail.id.toString(),
            serviceName = detail.title,
            categoryDisplayString = editorCategory.displayLabel,
            account =
                LoadFromExistingAccountParams(
                    id = detail.credentials?.id ?: "",
                    password = detail.credentials?.password ?: "",
                ),
            processing =
                LoadFromExistingProcessingParams(
                    message = detail.processing?.leaveMessage ?: "",
                    accountMethodName = accountProcessingMethodName,
                    informationMethodName = informationProcessingMethodName,
                    methods = if (!isGallery) actionItems else emptyList(),
                    galleryMethods = if (isGallery) actionItems else emptyList(),
                ),
            atmosphere = detail.playlist?.atmosphere,
            memorialVideoUrl = detail.playlist?.playlistDetailMemorialMedia?.videoUrl,
            memorialThumbnailUrl = detail.playlist?.playlistDetailMemorialMedia?.thumbnailUrl,
            memorialPhotoUrl = detail.playlist?.playlistDetailMemorialMedia?.photoUrl,
        )
    }

    fun buildPlaylistInput(
        playlistStateHolder: MemorialPlaylistStateHolder?,
        atmosphere: String = "",
        memorialPhotoUrl: String? = null,
        funeralVideoUrl: String? = null,
        funeralThumbnailUrl: String? = null,
    ): PlaylistInput {
        val songs =
            playlistStateHolder?.songs?.map { song ->
                SongInput(
                    id = song.id.toLongOrNull(),
                    title = song.title,
                    artist = song.artist,
                    coverUrl = song.albumCoverUrl,
                )
            } ?: emptyList()
        val memorialVideo =
            if (funeralVideoUrl.isNullOrBlank()) {
                null
            } else {
                MemorialVideoInput(
                    videoUrl = funeralVideoUrl,
                    thumbnailUrl = funeralThumbnailUrl.takeIf { !it.isNullOrBlank() },
                )
            }
        return PlaylistInput(
            atmosphere = atmosphere.ifEmpty { null },
            memorialPhotoUrl = memorialPhotoUrl?.takeIf { it.isNotBlank() },
            songs = songs,
            memorialVideo = memorialVideo,
        )
    }

    fun toServerProcessMethod(
        accountProcessingMethod: String,
        informationProcessingMethod: String,
    ): String {
        AccountProcessMethod
            .fromClientName(accountProcessingMethod)
            ?.let { return it.serverValue }
        val fallback = accountProcessingMethod.ifEmpty { informationProcessingMethod }
        return InfoProcessMethod.entries
            .find { it.clientName == fallback }
            ?.serverValue
            ?: fallback
    }

    fun buildCreateInput(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistStateHolder: MemorialPlaylistStateHolder?,
        funeralVideoUrl: String?,
        funeralThumbnailUrl: String?,
        memorialPhotoUrl: String?,
    ): CreateInput {
        val actions =
            payload.processingMethods.map { it.text } +
                payload.galleryProcessingMethods.map { it.text }
        val isSocial = category == EditorCategory.SOCIAL
        val processMethod =
            toServerProcessMethod(
                accountProcessingMethod = if (isSocial) payload.accountProcessingMethod else "",
                informationProcessingMethod = if (!isSocial) payload.informationProcessingMethod else "",
            )
        val leaveMessage = payload.message.ifEmpty { null }

        return when (category) {
            EditorCategory.GALLERY -> {
                val galleryActions = actions.ifEmpty { listOf("정보 전달") }
                CreateInput.Gallery(
                    CreateGalleryInput(
                        title = payload.serviceName,
                        processMethod = processMethod,
                        actions = galleryActions,
                        leaveMessage = leaveMessage,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            EditorCategory.MEMORIAL -> {
                val playlistInput =
                    buildPlaylistInput(
                        playlistStateHolder = playlistStateHolder,
                        atmosphere = payload.atmosphere,
                        memorialPhotoUrl = memorialPhotoUrl,
                        funeralVideoUrl = funeralVideoUrl,
                        funeralThumbnailUrl = funeralThumbnailUrl,
                    )
                CreateInput.Playlist(
                    CreatePlaylistInput(
                        title = payload.serviceName,
                        playlist = playlistInput,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            EditorCategory.SOCIAL -> {
                CreateInput.Social(
                    CreateSocialInput(
                        title = payload.serviceName,
                        processMethod = processMethod,
                        actions = actions,
                        leaveMessage = leaveMessage,
                        credentials =
                            CredentialsInput(
                                id = payload.accountId.takeIf { it.isNotEmpty() },
                                password = payload.password.takeIf { it.isNotEmpty() },
                            ),
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }
        }
    }

    fun buildUpdateInput(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistStateHolder: MemorialPlaylistStateHolder?,
        memorialMedia: MemorialMediaUrls,
    ): UpdateInput =
        if (category == EditorCategory.MEMORIAL) {
            UpdateInput(
                category = EditorCategory.MEMORIAL.serverValue,
                title = payload.serviceName,
                playlist =
                    buildPlaylistInput(
                        playlistStateHolder = playlistStateHolder,
                        atmosphere = payload.atmosphere,
                        memorialPhotoUrl = memorialMedia.memorialPhotoUrl,
                        funeralVideoUrl = memorialMedia.funeralVideoUrl,
                        funeralThumbnailUrl = memorialMedia.funeralThumbnailUrl,
                    ),
            )
        } else {
            buildNonMemorialUpdateInput(category, payload, selectedReceiverIds)
        }

    private fun buildNonMemorialUpdateInput(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
    ): UpdateInput {
        val actions =
            payload.processingMethods.map { it.text } +
                payload.galleryProcessingMethods.map { it.text }
        val isSocial = category == EditorCategory.SOCIAL
        val processMethod =
            toServerProcessMethod(
                accountProcessingMethod = if (isSocial) payload.accountProcessingMethod else "",
                informationProcessingMethod = if (!isSocial) payload.informationProcessingMethod else "",
            )
        return UpdateInput(
            category = category.serverValue,
            title = payload.serviceName,
            processMethod = processMethod.ifEmpty { null },
            actions = actions.ifEmpty { null },
            leaveMessage = payload.message.ifEmpty { null },
            credentials =
                if (isSocial) {
                    val id = payload.accountId.takeIf { it.isNotEmpty() }
                    val pw = payload.password.takeIf { it.isNotEmpty() }
                    if (id != null || pw != null) CredentialsInput(id = id, password = pw) else null
                } else {
                    null
                },
            receivers =
                if (category == EditorCategory.GALLERY) {
                    selectedReceiverIds.map { ReceiverRefInput(receiverId = it) }
                } else {
                    null
                },
            playlist = null,
        )
    }
}

/** Mapper에서 사용하는 생성 입력 sealed class. ViewModel에서 적절한 UseCase를 호출할 때 분기합니다. */
internal sealed class CreateInput {
    data class Social(
        val input: CreateSocialInput,
    ) : CreateInput()

    data class Gallery(
        val input: CreateGalleryInput,
    ) : CreateInput()

    data class Playlist(
        val input: CreatePlaylistInput,
    ) : CreateInput()
}

/**
 * Resolved memorial media URLs for performUpdate/performCreate.
 */
internal data class MemorialMediaUrls(
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
)
