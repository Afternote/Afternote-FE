package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload
import com.afternote.feature.afternote.domain.model.author.CreateSocialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.PlaylistSongPayload
import com.afternote.feature.afternote.domain.model.author.PlaylistWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessagesCodec
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.LastWishPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingAccountParams
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingParams
import com.afternote.feature.afternote.presentation.author.editor.model.LoadFromExistingProcessingParams
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem

private const val LAST_WISH_DEFAULT_CALM = "차분하고 조용하게 보내주세요."
private const val LAST_WISH_DEFAULT_BRIGHT = "슬퍼 하지 말고 밝고 따뜻하게 보내주세요."

/**
 * 에디터 폼 프리필·저장 페이로드용 Domain ↔ UI 매핑.
 *
 * 상세 화면의 AfternoteDetailSuccessMapper.kt 매퍼와 달리, 여기서는 조회 성공 직후 UI가 아니라 [EditorFormPrefill]·생성/수정 입력 조립이 중심이다.
 * `author/editor` 패키지 루트에 둔다.
 *
 * 추억 플레이리스트 곡 목록은 [com.afternote.feature.afternote.presentation.AfternoteHostViewModel.playlistSongs] SSOT의 스냅샷을
 * `playlistSongs: List<Song>`으로 받는다 (Compose 상태 홀더에 직접 의존하지 않는다).
 */
internal object AfternoteEditorFormMapper {
    fun buildEditorFormPrefill(detail: Detail): EditorFormPrefill = editorFormPrefillFromLoadParams(buildLoadFromExistingParams(detail))

    /**
     * [LoadFromExistingParams]의 문자열·분기를 해석해 폼에 바로 넣을 [EditorFormPrefill]을 만든다.
     * (Preview·테스트에서도 사용)
     */
    fun editorFormPrefillFromLoadParams(params: LoadFromExistingParams): EditorFormPrefill {
        val category = EditorCategory.fromDisplayLabel(params.categoryDisplayString)
        val messageBlocks = EditorMessagesCodec.parsePersistedToBlocks(params.processing.message)
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
            socialProcessingMethods = params.processing.socialMethods,
            galleryProcessingMethods = params.processing.galleryMethods,
            lastWishUpdate = lastWish,
            funeralVideoUrl = params.memorialVideoUrl,
            funeralThumbnailUrl = params.memorialThumbnailUrl,
            memorialPhotoUrl = params.memorialPhotoUrl,
            memorialPlaylistSongs = params.memorialSongs,
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
        val editorCategory = EditorCategory.fromServerValue(detail.category)
        val isGallery = editorCategory == EditorCategory.GALLERY
        val memorialSongs: List<Song> =
            if (editorCategory == EditorCategory.MEMORIAL) {
                detail.playlist?.songs?.mapIndexed { index, s ->
                    Song(
                        id = (s.id ?: index.toLong()).toString(),
                        title = s.title,
                        artist = s.artist,
                        albumCoverUrl = s.coverUrl,
                    )
                } ?: emptyList()
            } else {
                emptyList()
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
                    socialMethods = if (isGallery) emptyList() else actionItems,
                    galleryMethods = if (isGallery) actionItems else emptyList(),
                ),
            atmosphere = detail.playlist?.atmosphere,
            memorialVideoUrl = detail.playlist?.playlistDetailMemorialMedia?.videoUrl,
            memorialThumbnailUrl = detail.playlist?.playlistDetailMemorialMedia?.thumbnailUrl,
            memorialPhotoUrl = detail.playlist?.playlistDetailMemorialMedia?.photoUrl,
            memorialSongs = memorialSongs,
        )
    }

    fun buildPlaylistWritePayload(
        playlistSongs: List<Song>,
        atmosphere: String = "",
        memorialPhotoUrl: String? = null,
        funeralVideoUrl: String? = null,
        funeralThumbnailUrl: String? = null,
    ): PlaylistWritePayload {
        val songs =
            playlistSongs.map { song ->
                PlaylistSongPayload(
                    id = song.id.toLongOrNull(),
                    title = song.title,
                    artist = song.artist,
                    coverUrl = song.albumCoverUrl,
                )
            }
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

    fun buildCreateInput(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistSongs: List<Song>,
        funeralVideoUrl: String?,
        funeralThumbnailUrl: String?,
        memorialPhotoUrl: String?,
    ): CreateAfternoteInput {
        val actions =
            payload.processingMethods.map { it.text } +
                payload.galleryProcessingMethods.map { it.text }
        val leaveMessage = payload.message.ifBlank { null }

        return when (category) {
            EditorCategory.GALLERY -> {
                val galleryActions = actions.ifEmpty { listOf("정보 전달") }
                CreateAfternoteInput.Gallery(
                    CreateGalleryPayload(
                        title = payload.serviceName,
                        actions = galleryActions,
                        leaveMessage = leaveMessage,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            EditorCategory.MEMORIAL -> {
                val playlistPayload =
                    buildPlaylistWritePayload(
                        playlistSongs = playlistSongs,
                        atmosphere = payload.atmosphere,
                        memorialPhotoUrl = memorialPhotoUrl,
                        funeralVideoUrl = funeralVideoUrl,
                        funeralThumbnailUrl = funeralThumbnailUrl,
                    )
                CreateAfternoteInput.Playlist(
                    CreatePlaylistPayload(
                        title = payload.serviceName,
                        playlist = playlistPayload,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            EditorCategory.SOCIAL -> {
                CreateAfternoteInput.Social(
                    CreateSocialPayload(
                        title = payload.serviceName,
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

            // placeholder 카테고리는 Validator 에서 이미 차단되므로 여기 도달 시 호출자 버그.
            EditorCategory.BUSINESS, EditorCategory.ESTATE -> {
                error("Unimplemented category cannot be saved: $category")
            }
        }
    }

    fun buildUpdatePayload(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistSongs: List<Song>,
        memorialMedia: MemorialMediaUrls,
    ): AfternoteUpdatePayload =
        when (category) {
            EditorCategory.MEMORIAL -> {
                AfternoteUpdatePayload(
                    category = EditorCategory.MEMORIAL.serverValue,
                    title = payload.serviceName,
                    playlist =
                        buildPlaylistWritePayload(
                            playlistSongs = playlistSongs,
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

            // placeholder 카테고리는 Validator 에서 차단됨. 도달 시 호출자 버그.
            EditorCategory.BUSINESS, EditorCategory.ESTATE -> {
                error("Unimplemented category cannot be saved: $category")
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
        val hasCredentials = category == EditorCategory.SOCIAL
        val credentials =
            if (hasCredentials) {
                val id = payload.accountId.ifBlank { null }
                val pw = payload.password.ifBlank { null }
                if (id != null || pw != null) AfternoteAccountCredentials(id = id, password = pw) else null
            } else {
                null
            }
        return AfternoteUpdatePayload(
            category = category.serverValue,
            title = payload.serviceName,
            actions = actions.ifEmpty { null },
            leaveMessage = payload.message.ifBlank { null },
            credentials = credentials,
            receivers = selectedReceiverIds.map { ReceiverRefPayload(receiverId = it) },
            playlist = null,
        )
    }
}

/**
 * Resolved memorial media URLs for performUpdate/performCreate.
 */
internal data class MemorialMediaUrls(
    val funeralVideoUrl: String? = null,
    val funeralThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
)
