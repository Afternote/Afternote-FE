package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreatePlaylistPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.PlaylistSongPayload
import com.afternote.feature.afternote.domain.model.author.PlaylistWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorFormMapper.buildUpdatePayload
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessagesCodec
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.receiver.model.AfternoteEditorReceiver

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
    fun buildEditorFormPrefill(detail: Detail): EditorFormPrefill {
        val processingMethodItems =
            detail.processingMethods.mapIndexed { index, text ->
                ProcessingMethodItem(
                    id = (index + 1).toString(),
                    text = text,
                )
            }
        val editorCategory = EditorCategory.fromServerValue(detail.category)
        val memorialSongs: List<Song> =
            if (editorCategory == EditorCategory.MEMORIAL) {
                detail.memorial?.songs?.mapIndexed { index, s ->
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
        return EditorFormPrefill(
            loadedItemId = detail.id.toString(),
            serviceName = detail.title,
            category = editorCategory,
            accountId = detail.credentials?.id.orEmpty(),
            password = detail.credentials?.password.orEmpty(),
            leaveMessageBlocks = detail.leaveMessage?.let(EditorMessagesCodec::parsePersistedToBlocks).orEmpty(),
            processingMethods = processingMethodItems,
            memorialVideoUrl = detail.memorial?.media?.videoUrl,
            memorialThumbnailUrl = detail.memorial?.media?.thumbnailUrl,
            memorialPhotoUrl = detail.memorial?.media?.photoUrl,
            memorialPlaylistSongs = memorialSongs,
            receivers =
                detail.receivers.map { receiver ->
                    AfternoteEditorReceiver(
                        id = receiver.receiverId.toString(),
                        name = receiver.name,
                        label = receiver.relation,
                    )
                },
        )
    }

    fun buildPlaylistWritePayload(
        playlistSongs: List<Song>,
        memorialPhotoUrl: String? = null,
        memorialVideoUrl: String? = null,
        memorialThumbnailUrl: String? = null,
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
            memorialVideoUrl?.ifBlank { null }?.let { url ->
                MemorialVideoPayload(
                    videoUrl = url,
                    thumbnailUrl = memorialThumbnailUrl?.ifBlank { null },
                )
            }
        return PlaylistWritePayload(
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
        memorialVideoUrl: String?,
        memorialThumbnailUrl: String?,
        memorialPhotoUrl: String?,
    ): CreateAfternoteInput {
        val processingMethods = payload.processingMethods.map { it.text }
        val leaveMessage = payload.message.ifBlank { null }

        return when (category) {
            EditorCategory.GALLERY -> {
                val galleryMethods = processingMethods.ifEmpty { listOf("정보 전달") }
                CreateAfternoteInput.Gallery(
                    CreateGalleryPayload(
                        title = payload.serviceName,
                        processingMethods = galleryMethods,
                        leaveMessage = leaveMessage,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            EditorCategory.MEMORIAL -> {
                val playlistPayload =
                    buildPlaylistWritePayload(
                        playlistSongs = playlistSongs,
                        memorialPhotoUrl = memorialPhotoUrl,
                        memorialVideoUrl = memorialVideoUrl,
                        memorialThumbnailUrl = memorialThumbnailUrl,
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
                    buildAccountCreatePayload(payload, processingMethods, leaveMessage, selectedReceiverIds),
                )
            }

            // BUSINESS 는 서버 바디 스키마가 SOCIAL 과 동일(계정·처리 방법·남기실 말씀)해 [CreateAccountPayload] 를
            // 공유하고, category 문자열만 data 계층 매퍼에서 "BUSINESS" 로 실린다 (이슈 #467).
            EditorCategory.BUSINESS -> {
                CreateAfternoteInput.Business(
                    buildAccountCreatePayload(payload, processingMethods, leaveMessage, selectedReceiverIds),
                )
            }

            // placeholder 카테고리는 Validator 에서 이미 차단되므로 여기 도달 시 호출자 버그.
            EditorCategory.ESTATE -> {
                error("Unimplemented category cannot be saved: $category")
            }
        }
    }

    private fun buildAccountCreatePayload(
        payload: RegisterAfternotePayload,
        processingMethods: List<String>,
        leaveMessage: String?,
        selectedReceiverIds: List<Long>,
    ): CreateAccountPayload =
        CreateAccountPayload(
            title = payload.serviceName,
            processingMethods = processingMethods,
            leaveMessage = leaveMessage,
            credentials =
                AfternoteAccountCredentials(
                    id = payload.accountId.ifBlank { null },
                    password = payload.password.ifBlank { null },
                ),
            receiverIds = selectedReceiverIds,
        )

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
                            memorialPhotoUrl = memorialMedia.memorialPhotoUrl,
                            memorialVideoUrl = memorialMedia.memorialVideoUrl,
                            memorialThumbnailUrl = memorialMedia.memorialThumbnailUrl,
                        ),
                )
            }

            EditorCategory.GALLERY, EditorCategory.SOCIAL, EditorCategory.BUSINESS -> {
                buildActionsUpdatePayload(category, payload, selectedReceiverIds)
            }

            // placeholder 카테고리는 Validator 에서 차단됨. 도달 시 호출자 버그.
            EditorCategory.ESTATE -> {
                error("Unimplemented category cannot be saved: $category")
            }
        }

    /**
     * 처리 방법 기반 카테고리(SOCIAL·BUSINESS·GALLERY) 공용 update payload —
     * [AfternoteUpdatePayload.processingMethods] 를 채우고 계정형(SOCIAL·BUSINESS)만 credentials 를 싣는다.
     * MEMORIAL 은 [AfternoteUpdatePayload.playlist] 기반이라 [buildUpdatePayload] 의 별도 분기.
     */
    private fun buildActionsUpdatePayload(
        category: EditorCategory,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
    ): AfternoteUpdatePayload {
        val processingMethods = payload.processingMethods.map { it.text }
        val hasCredentials = category == EditorCategory.SOCIAL || category == EditorCategory.BUSINESS
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
            processingMethods = processingMethods.ifEmpty { null },
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
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
)
