package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.AfternoteUpdatePayload
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.DraftDetail
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.presentation.editor.AfternoteEditorFormMapper.buildUpdatePayload
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.editor.model.EditorCredentialsPrefill
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver

/**
 * 에디터 폼 프리필·저장 페이로드용 Domain ↔ UI 매핑.
 *
 * 상세 화면의 AfternoteDetailSuccessMapper.kt 매퍼와 달리, 여기서는 조회 성공 직후 UI가 아니라 [EditorFormPrefill]·생성/수정 입력 조립이 중심이다.
 * `author/editor` 패키지 루트에 둔다.
 *
 * 추억 플레이리스트 곡 목록은 flow-scoped 에디터 폼 SSOT의 스냅샷을
 * `playlistSongs: List<Song>`으로 받는다 (Compose 상태 홀더에 직접 의존하지 않는다).
 */
internal object AfternoteEditorFormMapper {
    fun buildEditorFormPrefill(detail: Detail): EditorFormPrefill =
        EditorFormPrefill(
            content = detail.content.toEditorContentPrefill(serviceName = detail.serviceName),
            leaveMessageBlocks = detail.leaveMessageBlocks.map(LeaveMessageBlock::toEditorBlock),
            receivers =
                detail.receivers.map { receiver ->
                    AfternoteEditorReceiver(
                        id = receiver.receiverId,
                        name = receiver.name,
                        label = receiver.relation,
                    )
                },
        )

    /**
     * 임시저장 상세 → 에디터 프리필 (#808).
     *
     * 발행 상세와 달리 종류별 값이 **아직 안 담긴 채로** 온다 — 그 «없음» 은 빈 입력칸이 되어야 하지
     * 실패가 아니다. [DraftDetail] 이 평평한 대신 종류를 필드로 들고 있어, 여기서 그 종류에 실제로
     * 존재하는 입력만 골라 담는다(발행 경로가 [DetailContent] 로 하는 일과 같다).
     */
    fun buildEditorFormPrefill(draft: DraftDetail): EditorFormPrefill =
        EditorFormPrefill(
            content = draft.toEditorContentPrefill(),
            leaveMessageBlocks = draft.leaveMessageBlocks.map(LeaveMessageBlock::toEditorBlock),
            receivers =
                draft.receivers.map { receiver ->
                    AfternoteEditorReceiver(
                        id = receiver.receiverId,
                        name = receiver.name,
                        label = receiver.relation,
                    )
                },
        )

    private fun DraftDetail.toEditorContentPrefill(): EditorContentPrefill =
        when (type) {
            AfternoteType.SOCIAL_NETWORK -> {
                EditorContentPrefill.SocialNetwork(
                    serviceName = serviceName,
                    credentials = credentials.toEditorCredentialsPrefill(),
                    processingMethods = processingMethods.toProcessingMethodItems(),
                )
            }

            AfternoteType.BUSINESS -> {
                EditorContentPrefill.Business(
                    serviceName = serviceName,
                    credentials = credentials.toEditorCredentialsPrefill(),
                    processingMethods = processingMethods.toProcessingMethodItems(),
                )
            }

            AfternoteType.GALLERY_AND_FILES -> {
                EditorContentPrefill.Gallery(
                    serviceName = serviceName,
                    processingMethods = processingMethods.toProcessingMethodItems(),
                )
            }

            AfternoteType.MEMORIAL -> {
                EditorContentPrefill.Memorial(
                    videoUrl = media.videoUrl,
                    thumbnailUrl = media.thumbnailUrl,
                    photoUrl = media.photoUrl,
                    playlistSongs =
                        songs.mapIndexed { index, song ->
                            Song(
                                selectionKey = "draft:$index",
                                title = song.title,
                                artist = song.artist,
                                albumCoverUrl = song.coverUrl,
                            )
                        },
                )
            }

            AfternoteType.ESTATE -> {
                EditorContentPrefill.Estate
            }
        }

    /**
     * 발행·임시저장 공용. 발행 상세는 서버가 non-blank 를 보장하므로 `orEmpty()` 가 무효타이고,
     * 계정 정보를 아직 안 쓴 임시저장(null)만 빈 입력칸으로 연다 — 「없음」과 「빈 문자열」의 구분은 폼에 없다.
     *
     * 널 허용 수신자 하나로 합친 이유는 JVM 소거다 — non-null 판과 시그니처가 같아 공존할 수 없다.
     */
    private fun DetailCredentials?.toEditorCredentialsPrefill() =
        EditorCredentialsPrefill(
            id = this?.id.orEmpty(),
            password = this?.password.orEmpty(),
        )

    private fun DetailContent.toEditorContentPrefill(serviceName: String): EditorContentPrefill =
        when (this) {
            is DetailContent.SocialNetwork -> {
                EditorContentPrefill.SocialNetwork(
                    serviceName = serviceName,
                    credentials = credentials.toEditorCredentialsPrefill(),
                    processingMethods = processingMethods.toProcessingMethodItems(),
                )
            }

            is DetailContent.Business -> {
                EditorContentPrefill.Business(
                    serviceName = serviceName,
                    credentials = credentials.toEditorCredentialsPrefill(),
                    processingMethods = processingMethods.toProcessingMethodItems(),
                )
            }

            is DetailContent.Gallery -> {
                EditorContentPrefill.Gallery(
                    serviceName = serviceName,
                    processingMethods = processingMethods.toProcessingMethodItems(),
                )
            }

            is DetailContent.Memorial -> {
                EditorContentPrefill.Memorial(
                    videoUrl = media.videoUrl,
                    thumbnailUrl = media.thumbnailUrl,
                    photoUrl = media.photoUrl,
                    playlistSongs =
                        songs.mapIndexed { index, song ->
                            Song(
                                selectionKey = "detail:$index",
                                title = song.title,
                                artist = song.artist,
                                albumCoverUrl = song.coverUrl,
                            )
                        },
                )
            }

            DetailContent.Estate -> {
                EditorContentPrefill.Estate
            }
        }

    private fun List<String>.toProcessingMethodItems(): List<ProcessingMethodItem> =
        mapIndexed { index, text ->
            ProcessingMethodItem(
                localId = index + 1,
                text = text,
            )
        }

    fun buildMemorialWritePayload(
        playlistSongs: List<Song>,
        memorialPhotoUrl: String? = null,
        memorialVideoUrl: String? = null,
        memorialThumbnailUrl: String? = null,
    ): MemorialWritePayload {
        val songs =
            playlistSongs.map { song ->
                MemorialSongPayload(
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
        return MemorialWritePayload(
            memorialPhotoUrl = memorialPhotoUrl?.ifBlank { null },
            songs = songs,
            memorialVideo = memorialVideo,
        )
    }

    fun buildCreateInput(
        type: AfternoteType,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistSongs: List<Song>,
        memorialVideoUrl: String?,
        memorialThumbnailUrl: String?,
        memorialPhotoUrl: String?,
    ): CreateAfternoteInput {
        val processingMethods = payload.processingMethods
        val leaveMessageBlocks = payload.messageBlocks.toLeaveMessageBlocks()

        return when (type) {
            AfternoteType.GALLERY_AND_FILES -> {
                CreateAfternoteInput.Gallery(
                    CreateGalleryPayload(
                        title = payload.serviceName,
                        processingMethods = processingMethods,
                        leaveMessageBlocks = leaveMessageBlocks,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            AfternoteType.MEMORIAL -> {
                val memorialPayload =
                    buildMemorialWritePayload(
                        playlistSongs = playlistSongs,
                        memorialPhotoUrl = memorialPhotoUrl,
                        memorialVideoUrl = memorialVideoUrl,
                        memorialThumbnailUrl = memorialThumbnailUrl,
                    )
                CreateAfternoteInput.Memorial(
                    CreateMemorialPayload(
                        title = payload.serviceName,
                        memorial = memorialPayload,
                        leaveMessageBlocks = leaveMessageBlocks,
                        receiverIds = selectedReceiverIds,
                    ),
                )
            }

            AfternoteType.SOCIAL_NETWORK -> {
                CreateAfternoteInput.Social(
                    buildAccountCreatePayload(payload, processingMethods, leaveMessageBlocks, selectedReceiverIds),
                )
            }

            // BUSINESS 는 서버 바디 스키마가 SOCIAL 과 동일(계정·처리 방법·남기실 말씀)해 [CreateAccountPayload] 를
            // 공유하고, category 문자열만 data 계층 매퍼에서 "BUSINESS" 로 실린다 (이슈 #467).
            AfternoteType.BUSINESS -> {
                CreateAfternoteInput.Business(
                    buildAccountCreatePayload(payload, processingMethods, leaveMessageBlocks, selectedReceiverIds),
                )
            }

            // placeholder 카테고리는 Validator 에서 이미 차단되므로 여기 도달 시 호출자 버그.
            AfternoteType.ESTATE -> {
                error("Unimplemented type cannot be saved: $type")
            }
        }
    }

    /**
     * 만들어 둔 생성 입력에 임시저장 여부만 얹는다 (#808).
     *
     * 종류별 빌더마다 인자를 늘리지 않는 이유는 그 값이 «무엇을 담았나» 가 아니라 «어느 버튼으로 저장하나» 라서다 —
     * 폼 내용과 무관하고, 저장 순간에만 정해진다.
     */
    fun withDraft(
        input: CreateAfternoteInput,
        isDraft: Boolean,
    ): CreateAfternoteInput =
        when (input) {
            is CreateAfternoteInput.Social -> CreateAfternoteInput.Social(input.payload.copy(isDraft = isDraft))
            is CreateAfternoteInput.Business -> CreateAfternoteInput.Business(input.payload.copy(isDraft = isDraft))
            is CreateAfternoteInput.Gallery -> CreateAfternoteInput.Gallery(input.payload.copy(isDraft = isDraft))
            is CreateAfternoteInput.Memorial -> CreateAfternoteInput.Memorial(input.payload.copy(isDraft = isDraft))
        }

    private fun buildAccountCreatePayload(
        payload: RegisterAfternotePayload,
        processingMethods: List<String>,
        leaveMessageBlocks: List<LeaveMessageBlock>,
        selectedReceiverIds: List<Long>,
    ): CreateAccountPayload =
        CreateAccountPayload(
            title = payload.serviceName,
            processingMethods = processingMethods,
            leaveMessageBlocks = leaveMessageBlocks,
            credentials =
                AfternoteAccountCredentials(
                    id = payload.accountId.ifBlank { null },
                    password = payload.password.ifBlank { null },
                ),
            receiverIds = selectedReceiverIds,
        )

    fun buildUpdatePayload(
        type: AfternoteType,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistSongs: List<Song>,
        memorialMedia: MemorialMediaUrls,
    ): AfternoteUpdatePayload =
        when (type) {
            AfternoteType.MEMORIAL -> {
                AfternoteUpdatePayload(
                    type = AfternoteType.MEMORIAL,
                    title = payload.serviceName,
                    leaveMessageBlocks = payload.messageBlocks.toLeaveMessageBlocks(),
                    memorial =
                        buildMemorialWritePayload(
                            playlistSongs = playlistSongs,
                            memorialPhotoUrl = memorialMedia.memorialPhotoUrl,
                            memorialVideoUrl = memorialMedia.memorialVideoUrl,
                            memorialThumbnailUrl = memorialMedia.memorialThumbnailUrl,
                        ),
                )
            }

            AfternoteType.GALLERY_AND_FILES, AfternoteType.SOCIAL_NETWORK, AfternoteType.BUSINESS -> {
                buildProcessingMethodsUpdatePayload(type, payload, selectedReceiverIds)
            }

            // placeholder 카테고리는 Validator 에서 차단됨. 도달 시 호출자 버그.
            AfternoteType.ESTATE -> {
                error("Unimplemented type cannot be saved: $type")
            }
        }

    /**
     * 처리 방법 기반 카테고리(SOCIAL·BUSINESS·GALLERY) 공용 update payload —
     * [AfternoteUpdatePayload.processingMethods] 를 채우고 계정형(SOCIAL·BUSINESS)만 credentials 를 싣는다.
     * MEMORIAL 은 [AfternoteUpdatePayload.memorial] 기반이라 [buildUpdatePayload] 의 별도 분기.
     */
    private fun buildProcessingMethodsUpdatePayload(
        type: AfternoteType,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
    ): AfternoteUpdatePayload {
        val processingMethods = payload.processingMethods
        val hasCredentials = type == AfternoteType.SOCIAL_NETWORK || type == AfternoteType.BUSINESS
        val credentials =
            if (hasCredentials) {
                val id = payload.accountId.ifBlank { null }
                val pw = payload.password.ifBlank { null }
                if (id != null || pw != null) AfternoteAccountCredentials(id = id, password = pw) else null
            } else {
                null
            }
        return AfternoteUpdatePayload(
            type = type,
            title = payload.serviceName,
            processingMethods = processingMethods.ifEmpty { null },
            leaveMessageBlocks = payload.messageBlocks.toLeaveMessageBlocks(),
            credentials = credentials,
            receivers = selectedReceiverIds.map { ReceiverRefPayload(receiverId = it) },
            memorial = null,
        )
    }
}

private fun LeaveMessageBlock.toEditorBlock(): EditorMessageTextBlock =
    EditorMessageTextBlock(
        title = title.orEmpty(),
        body = body,
        isRegistered = true,
    )

/**
 * 편집 블록을 서버로 보낼 도메인 블록으로 좁힌다.
 *
 * 에디터가 입력 전에도 띄워 두는 빈 칸은 버린다. 제목만 채운 블록은 여기 오기 전에
 * [AfternoteEditorValidator] 가 막으므로(서버가 본문을 필수로 검증한다) 버려서 입력을 잃는 경우는 없다.
 * 남는 블록이 없을 때 요청에서 필드를 뺄지는 data 계층 `toDto` 가 정한다.
 */
private fun List<EditorMessageTextBlock>.toLeaveMessageBlocks(): List<LeaveMessageBlock> =
    filter { it.body.isNotBlank() }
        .map { LeaveMessageBlock(title = it.title.trim().ifEmpty { null }, body = it.body.trim()) }

/**
 * Resolved memorial media URLs for performUpdate/performCreate.
 */
internal data class MemorialMediaUrls(
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
)
