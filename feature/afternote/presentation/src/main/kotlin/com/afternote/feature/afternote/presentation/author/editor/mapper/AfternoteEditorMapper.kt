package com.afternote.feature.afternote.presentation.author.editor.mapper

import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload
import com.afternote.feature.afternote.domain.model.author.CreateSocialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.PlaylistSongPayload
import com.afternote.feature.afternote.domain.model.author.PlaylistWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.presentation.author.editor.account.AccountProcessMethod
import com.afternote.feature.afternote.presentation.author.editor.account.InfoProcessMethod
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessagesCodec
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.model.LastWishPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingAccountParams
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingParams
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingProcessingParams
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem

private const val LAST_WISH_DEFAULT_CALM = "차분하고 조용하게 보내주세요."
private const val LAST_WISH_DEFAULT_BRIGHT = "슬퍼 하지 말고 밝고 따뜻하게 보내주세요."

/**
 * ViewModel ↔ Domain 간 데이터 변환 로직을 담당합니다.
 */
internal object AfternoteEditorMapper {
    fun buildEditorFormPrefill(detail: Detail): EditorFormPrefill = editorFormPrefillFromLoadParams(buildLoadFromExistingParams(detail))

    /**
     * [LoadFromExistingParams]의 문자열·분기를 해석해 폼에 바로 넣을 [EditorFormPrefill]을 만든다.
     * (Preview·테스트에서도 사용)
     */
    fun editorFormPrefillFromLoadParams(params: LoadFromExistingParams): EditorFormPrefill {
        val category = EditorCategory.fromDisplayLabel(params.categoryDisplayString)
        val messageBlocks = EditorMessagesCodec.parsePersistedToBlocks(params.processing.message)
        val accountPm =
            if (params.processing.accountMethodName.isNotEmpty()) {
                runCatching {
                    AccountProcessingMethod.valueOf(params.processing.accountMethodName)
                }.getOrDefault(AccountProcessingMethod.MEMORIAL_ACCOUNT)
            } else {
                null
            }
        val informationPm =
            if (params.processing.informationMethodName.isNotEmpty()) {
                val infoMethodName =
                    when (params.processing.informationMethodName) {
                        "TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER",
                        "ADDITIONAL",
                        -> "TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER"

                        else -> params.processing.informationMethodName
                    }
                runCatching {
                    InformationProcessingMethod.valueOf(infoMethodName)
                }.getOrDefault(InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER)
            } else {
                null
            }
        val lastWish =
            params.atmosphere?.let { atmosphereValue ->
                val trimmed = atmosphereValue.trim()
                when {
                    trimmed.isEmpty() -> LastWishPrefill(selectedKey = null, customText = "")
                    trimmed == LAST_WISH_DEFAULT_CALM -> LastWishPrefill(selectedKey = "calm", customText = "")
                    trimmed == LAST_WISH_DEFAULT_BRIGHT -> LastWishPrefill(selectedKey = "bright", customText = "")
                    else -> LastWishPrefill(selectedKey = "other", customText = trimmed)
                }
            }
        return EditorFormPrefill(
            loadedItemId = params.itemId,
            serviceName = params.serviceName,
            category = category,
            accountId = params.account.id,
            password = params.account.password,
            messageBlocks = messageBlocks,
            accountProcessingMethod = accountPm,
            informationProcessingMethod = informationPm,
            socialProcessingMethods = params.processing.methods,
            galleryProcessingMethods = params.processing.galleryMethods,
            lastWishUpdate = lastWish,
            funeralVideoUrl = params.memorialVideoUrl,
            funeralThumbnailUrl = params.memorialThumbnailUrl,
            memorialPhotoUrl = params.memorialPhotoUrl,
        )
    }

    fun buildLoadFromExistingParams(detail: Detail): LoadFromExistingParams {
        val actionItems =
            detail.processing?.actions?.mapIndexed { index, text ->
                ProcessingMethodItem(
                    id = (index + 1).toString(),
                    text = text,
                )
            } ?: emptyList()
        val processMethod = detail.processing?.method.orEmpty()
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
                    id = detail.credentials?.id.orEmpty(),
                    password = detail.credentials?.password.orEmpty(),
                ),
            processing =
                LoadFromExistingProcessingParams(
                    message = detail.processing?.leaveMessage.orEmpty(),
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

    fun buildPlaylistWritePayload(
        playlistStateHolder: MemorialPlaylistStateHolder?,
        atmosphere: String = "",
        memorialPhotoUrl: String? = null,
        funeralVideoUrl: String? = null,
        funeralThumbnailUrl: String? = null,
    ): PlaylistWritePayload {
        val songs =
            playlistStateHolder
                ?.songs
                ?.map { song ->
                    PlaylistSongPayload(
                        id = song.id.toLongOrNull(),
                        title = song.title,
                        artist = song.artist,
                        coverUrl = song.albumCoverUrl,
                    )
                }.orEmpty()
        val memorialVideo =
            funeralVideoUrl?.ifBlank { null }?.let { url ->
                MemorialVideoPayload(
                    videoUrl = url,
                    thumbnailUrl = funeralThumbnailUrl?.ifBlank { null },
                )
            }
        return PlaylistWritePayload(
            atmosphere = atmosphere.ifBlank { null },
            memorialPhotoUrl = memorialPhotoUrl?.ifBlank { null },
            songs = songs,
            memorialVideo = memorialVideo,
        )
    }

    fun toServerProcessMethod(
        accountProcessingMethod: String,
        informationProcessingMethod: String,
    ): String =
        AccountProcessMethod.fromClientName(accountProcessingMethod)?.serverValue
            ?: InfoProcessMethod.entries
                .find {
                    it.clientName == accountProcessingMethod.ifBlank { informationProcessingMethod }
                }?.serverValue
            ?: accountProcessingMethod.ifBlank { informationProcessingMethod }

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
        val leaveMessage = payload.message.ifBlank { null }

        return when (category) {
            EditorCategory.GALLERY -> {
                val galleryActions = actions.ifEmpty { listOf("정보 전달") }
                CreateInput.Gallery(
                    CreateGalleryPayload(
                        title = payload.serviceName,
                        processMethod = processMethod,
                        actions = galleryActions,
                        leaveMessage = leaveMessage,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            EditorCategory.MEMORIAL -> {
                val playlistPayload =
                    buildPlaylistWritePayload(
                        playlistStateHolder = playlistStateHolder,
                        atmosphere = payload.atmosphere,
                        memorialPhotoUrl = memorialPhotoUrl,
                        funeralVideoUrl = funeralVideoUrl,
                        funeralThumbnailUrl = funeralThumbnailUrl,
                    )
                CreateInput.Playlist(
                    CreatePlaylistPayload(
                        title = payload.serviceName,
                        playlist = playlistPayload,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            EditorCategory.SOCIAL -> {
                CreateInput.Social(
                    CreateSocialPayload(
                        title = payload.serviceName,
                        processMethod = processMethod,
                        actions = actions,
                        leaveMessage = leaveMessage,
                        credentials =
                            AfternoteAccountCredentials(
                                id = payload.accountId.ifBlank { null },
                                password = payload.password.ifBlank { null },
                            ),
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }
        }
    }

    fun buildUpdatePayload(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistStateHolder: MemorialPlaylistStateHolder?,
        memorialMedia: MemorialMediaUrls,
    ): AfternoteUpdatePayload =
        when (category) {
            EditorCategory.MEMORIAL -> {
                AfternoteUpdatePayload(
                    category = EditorCategory.MEMORIAL.serverValue,
                    title = payload.serviceName,
                    playlist =
                        buildPlaylistWritePayload(
                            playlistStateHolder = playlistStateHolder,
                            atmosphere = payload.atmosphere,
                            memorialPhotoUrl = memorialMedia.memorialPhotoUrl,
                            funeralVideoUrl = memorialMedia.funeralVideoUrl,
                            funeralThumbnailUrl = memorialMedia.funeralThumbnailUrl,
                        ),
                )
            }

            EditorCategory.GALLERY, EditorCategory.SOCIAL -> {
                buildNonMemorialUpdatePayload(category, payload, selectedReceiverIds)
            }
        }

    private fun buildNonMemorialUpdatePayload(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
    ): AfternoteUpdatePayload {
        val actions =
            payload.processingMethods.map { it.text } +
                payload.galleryProcessingMethods.map { it.text }
        val isSocial = category == EditorCategory.SOCIAL
        val processMethod =
            toServerProcessMethod(
                accountProcessingMethod = if (isSocial) payload.accountProcessingMethod else "",
                informationProcessingMethod = if (!isSocial) payload.informationProcessingMethod else "",
            )
        val credentials =
            if (isSocial) {
                val id = payload.accountId.ifBlank { null }
                val pw = payload.password.ifBlank { null }
                if (id != null || pw != null) AfternoteAccountCredentials(id = id, password = pw) else null
            } else {
                null
            }
        return AfternoteUpdatePayload(
            category = category.serverValue,
            title = payload.serviceName,
            processMethod = processMethod.ifBlank { null },
            actions = actions.ifEmpty { null },
            leaveMessage = payload.message.ifBlank { null },
            credentials = credentials,
            receivers = selectedReceiverIds.map { ReceiverRefPayload(receiverId = it) },
            playlist = null,
        )
    }
}

internal sealed interface CreateInput {
    data class Social(
        val payload: CreateSocialPayload,
    ) : CreateInput

    data class Gallery(
        val payload: CreateGalleryPayload,
    ) : CreateInput

    data class Playlist(
        val payload: CreatePlaylistPayload,
    ) : CreateInput
}

/**
 * Resolved memorial media URLs for performUpdate/performCreate.
 */
internal data class MemorialMediaUrls(
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
)
