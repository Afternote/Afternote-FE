package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock
import com.afternote.feature.afternote.domain.model.author.AfternoteAccountCredentials
import com.afternote.feature.afternote.domain.model.author.CreateAccountPayload
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.CreateGalleryPayload
import com.afternote.feature.afternote.domain.model.author.CreateMemorialPayload
import com.afternote.feature.afternote.domain.model.author.Detail
import com.afternote.feature.afternote.domain.model.author.DetailContent
import com.afternote.feature.afternote.domain.model.author.DetailCredentials
import com.afternote.feature.afternote.domain.model.author.FieldPatch
import com.afternote.feature.afternote.domain.model.author.MemorialPatchInput
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.model.author.UpdateAfternoteInput
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

    private fun DetailCredentials.toEditorCredentialsPrefill() =
        EditorCredentialsPrefill(
            id = id,
            password = password,
        )

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
    ): MemorialWritePayload =
        MemorialWritePayload(
            memorialPhotoUrl = memorialPhotoUrl?.ifBlank { null },
            songs = playlistSongs.map { it.toMemorialSongPayload() },
            memorialVideo = memorialVideoPayload(videoUrl = memorialVideoUrl, thumbnailUrl = memorialThumbnailUrl),
        )

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
    ): UpdateAfternoteInput {
        val current =
            buildEditorSnapshot(
                type = type,
                payload = payload,
                selectedReceiverIds = selectedReceiverIds,
                playlistSongs = playlistSongs,
                memorialMedia = memorialMedia,
            )
        val common =
            UpdateAfternoteInput(
                type = type,
                title = current.title.takeIf { it.trim() != baseline.title.trim() },
                leaveMessageBlocks = current.leaveMessageBlocks.takeIf { it != baseline.leaveMessageBlocks },
            )
        // 카테고리마다 그 카테고리가 가진 슬롯만 견준다. 없는 슬롯은 기본값(null)이라 요청에서 빠진다.
        return when (current) {
            is SocialNetworkSnapshot -> {
                val origin = baseline.requireSameCategory<SocialNetworkSnapshot>()
                common.copy(
                    processingMethods = changedProcessingMethods(current.processingMethods, origin.processingMethods),
                    credentials = changedCredentials(current.credentials, origin.credentials),
                    receiverIds = changedReceivers(current.receiverIds, origin.receiverIds),
                )
            }

            is BusinessSnapshot -> {
                val origin = baseline.requireSameCategory<BusinessSnapshot>()
                common.copy(
                    processingMethods = changedProcessingMethods(current.processingMethods, origin.processingMethods),
                    credentials = changedCredentials(current.credentials, origin.credentials),
                    receiverIds = changedReceivers(current.receiverIds, origin.receiverIds),
                )
            }

            is GallerySnapshot -> {
                val origin = baseline.requireSameCategory<GallerySnapshot>()
                common.copy(
                    processingMethods = changedProcessingMethods(current.processingMethods, origin.processingMethods),
                    receiverIds = changedReceivers(current.receiverIds, origin.receiverIds),
                )
            }

            // 추억 노트 수정 페이로드는 수신자를 싣지 않는다.
            is MemorialSnapshot -> {
                common.copy(memorial = changedMemorial(current, baseline.requireSameCategory<MemorialSnapshot>()))
            }
        }
    }

    /**
     * 저장 카테고리와 기준 카테고리는 같아야 한다. 수정 화면은 카테고리를 바꿀 수 없고, 호출부는 기준을
     * 만든 상세의 카테고리로 저장한다. 어긋났다면 슬롯을 견줄 수 없으므로 호출자 버그다.
     */
    private inline fun <reified T : AfternoteEditorSnapshot> AfternoteEditorSnapshot.requireSameCategory(): T =
        checkNotNull(this as? T) { "수정 기준 스냅샷의 카테고리가 저장 카테고리와 다릅니다" }

    /**
     * 빈 문자열을 걷어내지 않는다. 서버는 actions 원소를 검증 없이 저장하므로 `[""]` 가 실제로 남아
     * 있을 수 있고, 그 행을 지운 저장이 양쪽 정규화로 상쇄되면 삭제가 사라진다.
     */
    private fun changedProcessingMethods(
        current: List<String>,
        baseline: List<String>,
    ): List<String>? = current.takeIf { it != baseline }

    /** 순서는 뜻을 갖지 않으므로 정렬해 견주고, 실을 때는 폼 순서 그대로 보낸다. */
    private fun changedReceivers(
        current: List<Long>,
        baseline: List<Long>,
    ): List<Long>? = current.takeIf { it.sorted() != baseline.sorted() }

    /** 계정 정보는 서버가 id·비밀번호를 독립으로 갱신하므로 슬롯별로 재고, 둘 다 그대로면 통째로 뺀다. */
    private fun changedCredentials(
        current: CredentialsSnapshot,
        baseline: CredentialsSnapshot,
    ): AfternoteAccountCredentials? {
        val id = current.id.takeIf { it != baseline.id }
        val password = current.password.takeIf { it != baseline.password }
        return if (id == null && password == null) null else AfternoteAccountCredentials(id = id, password = password)
    }

    /** 플레이리스트는 슬롯 셋을 따로 재고, 하나도 안 바뀌었으면 `playlist` 키 자체를 내보내지 않는다. */
    private fun changedMemorial(
        current: MemorialSnapshot,
        baseline: MemorialSnapshot,
    ): MemorialPatchInput? {
        val patch =
            MemorialPatchInput(
                memorialPhotoUrl = FieldPatch.changedOrUnchanged(current.photoUrl, baseline.photoUrl),
                songs = current.songs.takeIf { it != baseline.songs },
                memorialVideo = FieldPatch.changedOrUnchanged(current.video, baseline.video),
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
        val title = detail.serviceName
        val leaveMessageBlocks = detail.leaveMessageBlocks.normalizedForDiff()
        val receiverIds = detail.receivers.map { it.receiverId }
        return when (val content = detail.content) {
            is DetailContent.SocialNetwork -> {
                SocialNetworkSnapshot(
                    title = title,
                    leaveMessageBlocks = leaveMessageBlocks,
                    credentials = content.credentials.toCredentialsSnapshot(),
                    processingMethods = content.processingMethods,
                    receiverIds = receiverIds,
                )
            }

            is DetailContent.Business -> {
                BusinessSnapshot(
                    title = title,
                    leaveMessageBlocks = leaveMessageBlocks,
                    credentials = content.credentials.toCredentialsSnapshot(),
                    processingMethods = content.processingMethods,
                    receiverIds = receiverIds,
                )
            }

            is DetailContent.Gallery -> {
                GallerySnapshot(
                    title = title,
                    leaveMessageBlocks = leaveMessageBlocks,
                    processingMethods = content.processingMethods,
                    receiverIds = receiverIds,
                )
            }

            is DetailContent.Memorial -> {
                MemorialSnapshot(
                    title = title,
                    leaveMessageBlocks = leaveMessageBlocks,
                    photoUrl = content.media.photoUrl?.ifBlank { null },
                    video = content.media.toVideoPayload(),
                    songs =
                        content.songs.map { song ->
                            MemorialSongPayload(title = song.title, artist = song.artist, coverUrl = song.coverUrl)
                        },
                )
            }

            // 저장은 막히지만 수정 진입은 되므로 기준은 만든다.
            DetailContent.Estate -> {
                EstateSnapshot(title = title, leaveMessageBlocks = leaveMessageBlocks)
            }
        }
    }

    /** 현재 폼을 기준 스냅샷과 **같은 어휘**로 옮긴다 — 그래야 슬롯끼리 견줄 수 있다. */
    private fun buildEditorSnapshot(
        type: AfternoteType,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistSongs: List<Song>,
        memorialMedia: MemorialMediaUrls,
    ): SavableEditorSnapshot {
        val title = payload.serviceName
        val leaveMessageBlocks = payload.messageBlocks.toLeaveMessageBlocks().normalizedForDiff()
        return when (type) {
            AfternoteType.SOCIAL_NETWORK -> {
                SocialNetworkSnapshot(
                    title = title,
                    leaveMessageBlocks = leaveMessageBlocks,
                    credentials = payload.toCredentialsSnapshot(),
                    processingMethods = payload.processingMethods,
                    receiverIds = selectedReceiverIds,
                )
            }

            AfternoteType.BUSINESS -> {
                BusinessSnapshot(
                    title = title,
                    leaveMessageBlocks = leaveMessageBlocks,
                    credentials = payload.toCredentialsSnapshot(),
                    processingMethods = payload.processingMethods,
                    receiverIds = selectedReceiverIds,
                )
            }

            AfternoteType.GALLERY_AND_FILES -> {
                GallerySnapshot(
                    title = title,
                    leaveMessageBlocks = leaveMessageBlocks,
                    processingMethods = payload.processingMethods,
                    receiverIds = selectedReceiverIds,
                )
            }

            AfternoteType.MEMORIAL -> {
                MemorialSnapshot(
                    title = title,
                    leaveMessageBlocks = leaveMessageBlocks,
                    photoUrl = memorialMedia.memorialPhotoUrl?.ifBlank { null },
                    video = memorialMedia.toVideoPayload(),
                    songs = playlistSongs.map { it.toMemorialSongPayload() },
                )
            }

            // placeholder 카테고리는 Validator 에서 차단됨. 도달 시 호출자 버그.
            AfternoteType.ESTATE -> {
                error("Unimplemented type cannot be saved: $type")
            }
        }
    }

    private fun DetailCredentials.toCredentialsSnapshot() =
        CredentialsSnapshot(id = id.ifBlank { null }, password = password.ifBlank { null })

    private fun RegisterAfternotePayload.toCredentialsSnapshot() =
        CredentialsSnapshot(id = accountId.ifBlank { null }, password = password.ifBlank { null })

    private fun Song.toMemorialSongPayload() = MemorialSongPayload(title = title, artist = artist, coverUrl = albumCoverUrl)

    private fun MemorialMedia.toVideoPayload(): MemorialVideoPayload? =
        memorialVideoPayload(videoUrl = videoUrl, thumbnailUrl = thumbnailUrl)

    private fun MemorialMediaUrls.toVideoPayload(): MemorialVideoPayload? =
        memorialVideoPayload(videoUrl = memorialVideoUrl, thumbnailUrl = memorialThumbnailUrl)

    /** 영상 주소가 비었으면 영상 슬롯 자체가 없다. 썸네일만 남은 값은 싣지 않는다. */
    private fun memorialVideoPayload(
        videoUrl: String?,
        thumbnailUrl: String?,
    ): MemorialVideoPayload? =
        videoUrl?.ifBlank { null }?.let { url ->
            MemorialVideoPayload(videoUrl = url, thumbnailUrl = thumbnailUrl?.ifBlank { null })
        }
}

/**
 * 현재 폼과 서버 원본을 견주기 위한 **같은 어휘의 스냅샷** (#1617).
 *
 * 「무엇을 보낼까」가 아니라 「무엇이 달라졌나」만 판정하는 자료다. 그래서 wire 표현(키 생략·명시적
 * null·빈 배열)을 담지 않고, 슬롯마다 의미값 하나씩만 든다. 카테고리마다 그 카테고리가 가진 슬롯만
 * 들며, 분류는 [DetailContent] 와 같다.
 */
internal sealed interface AfternoteEditorSnapshot {
    val title: String
    val leaveMessageBlocks: List<LeaveMessageBlock>
}

/** 수정 저장까지 가는 카테고리. [EstateSnapshot] 은 기준으로만 존재한다. */
private sealed interface SavableEditorSnapshot : AfternoteEditorSnapshot

private data class SocialNetworkSnapshot(
    override val title: String,
    override val leaveMessageBlocks: List<LeaveMessageBlock>,
    val credentials: CredentialsSnapshot,
    val processingMethods: List<String>,
    val receiverIds: List<Long>,
) : SavableEditorSnapshot

private data class BusinessSnapshot(
    override val title: String,
    override val leaveMessageBlocks: List<LeaveMessageBlock>,
    val credentials: CredentialsSnapshot,
    val processingMethods: List<String>,
    val receiverIds: List<Long>,
) : SavableEditorSnapshot

private data class GallerySnapshot(
    override val title: String,
    override val leaveMessageBlocks: List<LeaveMessageBlock>,
    val processingMethods: List<String>,
    val receiverIds: List<Long>,
) : SavableEditorSnapshot

private data class MemorialSnapshot(
    override val title: String,
    override val leaveMessageBlocks: List<LeaveMessageBlock>,
    val photoUrl: String?,
    val video: MemorialVideoPayload?,
    val songs: List<MemorialSongPayload>,
) : SavableEditorSnapshot

private data class EstateSnapshot(
    override val title: String,
    override val leaveMessageBlocks: List<LeaveMessageBlock>,
) : AfternoteEditorSnapshot

/** 빈 칸은 `null` 로 맞춰 둔다. 서버의 빈 값과 폼의 빈 입력이 같은 값으로 견줘지게. */
private data class CredentialsSnapshot(
    val id: String?,
    val password: String?,
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
