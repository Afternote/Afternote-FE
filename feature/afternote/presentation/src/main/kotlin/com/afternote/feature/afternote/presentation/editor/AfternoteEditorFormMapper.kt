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
import com.afternote.feature.afternote.domain.model.author.FieldPatch
import com.afternote.feature.afternote.domain.model.author.MemorialPatchPayload
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.domain.model.author.playlist.MemorialMedia
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
     * 발행·임시저장 공용. 미작성 계정 정보는 빈 입력칸으로 연다.
     * 발행 필수값의 검증은 상세 계약을 담당하는 data 매퍼의 몫이다.
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

    private fun buildMemorialWritePayload(
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

    /**
     * 수정 요청 페이로드를 조립한다 — **[baseline] 과 달라진 슬롯만 싣는다** (#1617).
     *
     * 폼 전체 스냅샷을 매번 통째로 보내면, 에디터를 연 뒤 서버가 바뀐 경우 사용자가 만진 적도 없는
     * 필드가 낡은 값으로 덮인다. 서버는 「키 없음 = 유지」로 읽으므로, 안 건드린 슬롯을 아예 빼는
     * 것으로 그 사고를 구조적으로 없앤다.
     *
     * **판정 단위는 서버가 반영하는 단위와 같다.** 객체를 통째로 재면 그 안에서 안 건드린 형제
     * 슬롯이 낡은 값째 딸려 나가므로, 계정 정보는 id·비밀번호를 따로, 플레이리스트는 사진·영상·곡을
     * 따로 잰다.
     *
     * [baseline] 은 **필수**다. 기준 없이 조립할 수 있게 두면 상세 조회가 실패한 화면에서 빈 폼이
     * 그대로 전량 PATCH 로 나가 이 이슈가 잡으려던 삭제 사고를 되풀이한다. 기준이 없을 때 저장을
     * 막는 것은 호출부([AfternoteEditorViewModel])의 몫이다.
     */
    fun buildUpdatePayload(
        type: AfternoteType,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistSongs: List<Song>,
        memorialMedia: MemorialMediaUrls,
        baseline: AfternoteEditorSnapshot,
    ): AfternoteUpdatePayload {
        if (type == AfternoteType.ESTATE) {
            // placeholder 카테고리는 Validator 에서 차단됨. 도달 시 호출자 버그.
            error("Unimplemented type cannot be saved: $type")
        }
        val current =
            buildEditorSnapshot(
                type = type,
                payload = payload,
                selectedReceiverIds = selectedReceiverIds,
                playlistSongs = playlistSongs,
                memorialMedia = memorialMedia,
            )
        return AfternoteUpdatePayload(
            type = type,
            title = current.title.takeIf { it.trim() != baseline.title.trim() },
            // 빈 문자열을 걷어내지 않는다 — 서버는 actions 원소를 검증 없이 저장하므로 `[""]` 가
            // 실제로 남아 있을 수 있고, 그 행을 지운 저장이 양쪽 정규화로 상쇄되면 삭제가 사라진다.
            processingMethods = current.processingMethods.takeIf { it != baseline.processingMethods },
            leaveMessageBlocks = current.leaveMessageBlocks.takeIf { it != baseline.leaveMessageBlocks },
            credentials = diffCredentials(current, baseline),
            // 순서는 뜻을 갖지 않으므로 정렬해 견주고, 실을 때는 폼 순서 그대로 보낸다.
            receivers =
                current.receiverIds
                    ?.takeIf { it.sorted() != baseline.receiverIds?.sorted() }
                    ?.map { ReceiverRefPayload(receiverId = it) },
            memorial = diffMemorial(current, baseline),
        )
    }

    /** 계정 정보는 서버가 id·비밀번호를 독립으로 갱신하므로 슬롯별로 재고, 둘 다 그대로면 통째로 뺀다. */
    private fun diffCredentials(
        current: AfternoteEditorSnapshot,
        baseline: AfternoteEditorSnapshot,
    ): AfternoteAccountCredentials? {
        val id = current.credentialsId.takeIf { it != baseline.credentialsId }
        val password = current.credentialsPassword.takeIf { it != baseline.credentialsPassword }
        return if (id == null && password == null) null else AfternoteAccountCredentials(id = id, password = password)
    }

    /** 플레이리스트는 슬롯 셋을 따로 재고, 하나도 안 바뀌었으면 `playlist` 키 자체를 내보내지 않는다. */
    private fun diffMemorial(
        current: AfternoteEditorSnapshot,
        baseline: AfternoteEditorSnapshot,
    ): MemorialPatchPayload? {
        if (current.type != AfternoteType.MEMORIAL) return null
        val patch =
            MemorialPatchPayload(
                memorialPhotoUrl =
                    FieldPatch.changedOrUnchanged(current.memorialPhotoUrl, baseline.memorialPhotoUrl),
                songs = current.songs.takeIf { it != baseline.songs },
                memorialVideo =
                    FieldPatch.changedOrUnchanged(current.memorialVideo, baseline.memorialVideo),
            )
        return patch.takeUnless { it.isUnchanged }
    }

    /**
     * 수정 진입 시 받은 상세를 **「서버가 지금 들고 있는 값」** 스냅샷으로 옮긴다.
     *
     * [buildEditorSnapshot] 이 만드는 현재 폼 스냅샷과 같은 어휘라 슬롯끼리 바로 견줄 수 있다.
     * 프리필([buildEditorFormPrefill])을 거치지 않고 [Detail] 에서 직접 만드는 이유는, 프리필이
     * 화면 표시용으로 값을 한 번 가공하기 때문이다 — 비교 기준은 가공 전 원본이어야 한다.
     */
    fun buildUpdateBaseline(detail: Detail): AfternoteEditorSnapshot {
        val content = detail.content
        val memorial = content as? DetailContent.Memorial
        return AfternoteEditorSnapshot(
            type = content.type,
            title = detail.serviceName,
            processingMethods =
                when (content) {
                    is DetailContent.SocialNetwork -> content.processingMethods
                    is DetailContent.Business -> content.processingMethods
                    is DetailContent.Gallery -> content.processingMethods
                    is DetailContent.Memorial, DetailContent.Estate -> null
                },
            leaveMessageBlocks = detail.leaveMessageBlocks.normalizedForDiff(),
            credentialsId = content.detailCredentials()?.id?.ifBlank { null },
            credentialsPassword = content.detailCredentials()?.password?.ifBlank { null },
            // 추억 노트 수정 페이로드는 수신자를 싣지 않으므로 기준도 같은 자리를 비워 둔다.
            receiverIds =
                when (content) {
                    is DetailContent.Memorial, DetailContent.Estate -> {
                        null
                    }

                    is DetailContent.SocialNetwork, is DetailContent.Business, is DetailContent.Gallery -> {
                        detail.receivers.map { it.receiverId }
                    }
                },
            memorialPhotoUrl = memorial?.media?.photoUrl?.ifBlank { null },
            memorialVideo = memorial?.media?.toVideoPayload(),
            songs =
                memorial?.songs?.map { song ->
                    MemorialSongPayload(title = song.title, artist = song.artist, coverUrl = song.coverUrl)
                },
        )
    }

    /** 미작성 값도 표시용 기본값으로 바꾸기 전 서버 상세 그대로 비교 기준에 보존한다. */
    fun buildUpdateBaseline(detail: DraftDetail): AfternoteEditorSnapshot {
        val isMemorial = detail.type == AfternoteType.MEMORIAL
        val hasCredentials = detail.type == AfternoteType.SOCIAL_NETWORK || detail.type == AfternoteType.BUSINESS
        return AfternoteEditorSnapshot(
            type = detail.type,
            title = detail.serviceName,
            processingMethods = if (isMemorial) null else detail.processingMethods,
            leaveMessageBlocks = detail.leaveMessageBlocks.normalizedForDiff(),
            credentialsId = if (hasCredentials) detail.credentials?.id?.ifBlank { null } else null,
            credentialsPassword = if (hasCredentials) detail.credentials?.password?.ifBlank { null } else null,
            receiverIds = if (isMemorial) null else detail.receivers.map { it.receiverId },
            memorialPhotoUrl = if (isMemorial) detail.media.photoUrl?.ifBlank { null } else null,
            memorialVideo = if (isMemorial) detail.media.toVideoPayload() else null,
            songs =
                if (isMemorial) {
                    detail.songs.map { song ->
                        MemorialSongPayload(title = song.title, artist = song.artist, coverUrl = song.coverUrl)
                    }
                } else {
                    null
                },
        )
    }

    /** 현재 폼을 기준 스냅샷과 **같은 어휘**로 옮긴다 — 그래야 슬롯끼리 견줄 수 있다. */
    private fun buildEditorSnapshot(
        type: AfternoteType,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistSongs: List<Song>,
        memorialMedia: MemorialMediaUrls,
    ): AfternoteEditorSnapshot {
        val isMemorial = type == AfternoteType.MEMORIAL
        val hasCredentials = type == AfternoteType.SOCIAL_NETWORK || type == AfternoteType.BUSINESS
        return AfternoteEditorSnapshot(
            type = type,
            title = payload.serviceName,
            processingMethods = if (isMemorial) null else payload.processingMethods,
            leaveMessageBlocks = payload.messageBlocks.toLeaveMessageBlocks().normalizedForDiff(),
            credentialsId = if (hasCredentials) payload.accountId.ifBlank { null } else null,
            credentialsPassword = if (hasCredentials) payload.password.ifBlank { null } else null,
            receiverIds = if (isMemorial) null else selectedReceiverIds,
            memorialPhotoUrl = if (isMemorial) memorialMedia.memorialPhotoUrl?.ifBlank { null } else null,
            memorialVideo = if (isMemorial) memorialMedia.toVideoPayload() else null,
            songs =
                if (isMemorial) {
                    playlistSongs.map { song ->
                        MemorialSongPayload(title = song.title, artist = song.artist, coverUrl = song.albumCoverUrl)
                    }
                } else {
                    null
                },
        )
    }

    private fun DetailContent.detailCredentials(): DetailCredentials? =
        when (this) {
            is DetailContent.SocialNetwork -> credentials
            is DetailContent.Business -> credentials
            is DetailContent.Gallery, is DetailContent.Memorial, DetailContent.Estate -> null
        }

    private fun MemorialMedia.toVideoPayload(): MemorialVideoPayload? =
        videoUrl?.ifBlank { null }?.let { url ->
            MemorialVideoPayload(videoUrl = url, thumbnailUrl = thumbnailUrl?.ifBlank { null })
        }

    private fun MemorialMediaUrls.toVideoPayload(): MemorialVideoPayload? =
        memorialVideoUrl?.ifBlank { null }?.let { url ->
            MemorialVideoPayload(videoUrl = url, thumbnailUrl = memorialThumbnailUrl?.ifBlank { null })
        }
}

/**
 * 현재 폼과 서버 원본을 견주기 위한 **같은 어휘의 스냅샷** (#1617).
 *
 * 「무엇을 보낼까」가 아니라 「무엇이 달라졌나」만 판정하는 자료다. 그래서 wire 표현(키 생략·명시적
 * null·빈 배열)을 담지 않고, 슬롯마다 의미값 하나씩만 든다. 카테고리에 없는 슬롯은 `null` 로 비워
 * 양쪽이 「둘 다 없음」으로 맞아떨어지게 한다.
 */
internal data class AfternoteEditorSnapshot(
    val type: AfternoteType,
    val title: String,
    val processingMethods: List<String>?,
    val leaveMessageBlocks: List<LeaveMessageBlock>,
    val credentialsId: String?,
    val credentialsPassword: String?,
    val receiverIds: List<Long>?,
    val memorialPhotoUrl: String?,
    val memorialVideo: MemorialVideoPayload?,
    val songs: List<MemorialSongPayload>?,
)

/**
 * 남기실 말씀을 비교용 정규형으로 좁힌다.
 *
 * 폼에서 도메인으로 옮길 때 이미 앞뒤 공백을 떼므로([toLeaveMessageBlocks]), 서버 원본도 같은 모양으로
 * 맞춰야 「공백만 다른 같은 값」이 변경으로 잡히지 않는다. 본문이 빈 블록은 서버가 400 으로 거절하고
 * (`AfternoteValidationCommons.validateLeaveMessage`) 응답 파싱도 걸러 내므로 양쪽에 존재하지 않는다 —
 * 그래서 여기서 원소를 **버리지는 않는다.** 버리기 시작하면 「지웠다」가 상쇄돼 사라진다.
 */
private fun List<LeaveMessageBlock>.normalizedForDiff(): List<LeaveMessageBlock> =
    map { LeaveMessageBlock(title = it.title?.trim()?.ifEmpty { null }, body = it.body.trim()) }

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
