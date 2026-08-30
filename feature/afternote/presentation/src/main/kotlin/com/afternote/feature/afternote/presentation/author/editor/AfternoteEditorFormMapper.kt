package com.afternote.feature.afternote.presentation.author.editor

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
import com.afternote.feature.afternote.domain.model.author.MemorialSongPayload
import com.afternote.feature.afternote.domain.model.author.MemorialVideoPayload
import com.afternote.feature.afternote.domain.model.author.MemorialWritePayload
import com.afternote.feature.afternote.domain.model.author.ReceiverRefPayload
import com.afternote.feature.afternote.presentation.author.editor.AfternoteEditorFormMapper.buildUpdatePayload
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCredentialsPrefill
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
                    videoUrl = memorial.media.videoUrl,
                    thumbnailUrl = memorial.media.thumbnailUrl,
                    photoUrl = memorial.media.photoUrl,
                    playlistSongs =
                        memorial.songs.mapIndexed { index, song ->
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
     * 수정 요청 페이로드를 조립한다 — **[baseline] 과 달라진 필드만 싣는다** (#1617).
     *
     * 폼 전체 스냅샷을 매번 통째로 보내면, 에디터를 연 뒤 서버가 바뀐 경우 사용자가 만진 적도 없는
     * 필드가 낡은 값으로 덮인다. 서버는 「키 없음 = 유지」로 읽으므로, 안 건드린 필드를 아예 빼는
     * 것으로 그 사고를 구조적으로 없앤다.
     *
     * @param baseline 수정 진입 시 받은 상세 응답을 [buildUpdateBaseline] 로 옮긴 **원본 스냅샷**.
     *   `null`(상세를 아직/끝내 못 받음)이면 비교할 기준이 없으므로 종전처럼 전량을 싣는다 —
     *   덜 보내다 사용자의 편집을 잃는 것보다 낫다.
     */
    fun buildUpdatePayload(
        type: AfternoteType,
        payload: RegisterAfternotePayload,
        selectedReceiverIds: List<Long>,
        playlistSongs: List<Song>,
        memorialMedia: MemorialMediaUrls,
        baseline: AfternoteUpdatePayload?,
    ): AfternoteUpdatePayload {
        val full =
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
        return full.omittingFieldsUnchangedFrom(baseline)
    }

    /**
     * 수정 진입 시 받은 상세를 **「서버가 지금 들고 있는 값」 그대로의 수정 페이로드**로 옮긴다.
     *
     * [buildUpdatePayload] 가 만드는 현재 폼 페이로드와 **같은 어휘**라, 필드끼리 바로 견줄 수 있다.
     * 프리필([buildEditorFormPrefill])을 거치지 않고 [Detail] 에서 직접 만드는 이유는, 프리필이
     * 화면 표시용으로 값을 한 번 가공하기 때문이다 — 비교 기준은 가공 전 원본이어야 한다.
     */
    fun buildUpdateBaseline(detail: Detail): AfternoteUpdatePayload {
        val content = detail.content
        return AfternoteUpdatePayload(
            type = content.type,
            title = detail.serviceName,
            processingMethods =
                when (content) {
                    is DetailContent.SocialNetwork -> content.processingMethods
                    is DetailContent.Business -> content.processingMethods
                    is DetailContent.Gallery -> content.processingMethods
                    is DetailContent.Memorial, DetailContent.Estate -> null
                },
            leaveMessageBlocks = detail.leaveMessageBlocks,
            credentials =
                when (content) {
                    is DetailContent.SocialNetwork -> content.credentials.toAccountCredentials()
                    is DetailContent.Business -> content.credentials.toAccountCredentials()
                    is DetailContent.Gallery, is DetailContent.Memorial, DetailContent.Estate -> null
                },
            // 추억 노트 수정 페이로드는 수신자를 싣지 않으므로([buildUpdatePayload] 의 MEMORIAL 분기)
            // 기준도 같은 자리를 비워 둔다 — 그래야 「양쪽 다 없음」으로 맞아떨어진다.
            receivers =
                when (content) {
                    is DetailContent.Memorial, DetailContent.Estate -> {
                        null
                    }

                    is DetailContent.SocialNetwork, is DetailContent.Business, is DetailContent.Gallery -> {
                        detail.receivers.map { ReceiverRefPayload(receiverId = it.receiverId) }
                    }
                },
            memorial =
                (content as? DetailContent.Memorial)?.let { memorialContent ->
                    MemorialWritePayload(
                        memorialPhotoUrl = memorialContent.memorial.media.photoUrl,
                        songs =
                            memorialContent.memorial.songs.map { song ->
                                MemorialSongPayload(
                                    title = song.title,
                                    artist = song.artist,
                                    coverUrl = song.coverUrl,
                                )
                            },
                        memorialVideo =
                            memorialContent.memorial.media.videoUrl?.ifBlank { null }?.let { url ->
                                MemorialVideoPayload(
                                    videoUrl = url,
                                    thumbnailUrl =
                                        memorialContent.memorial.media.thumbnailUrl
                                            ?.ifBlank { null },
                                )
                            },
                    )
                },
        )
    }

    private fun DetailCredentials.toAccountCredentials() =
        AfternoteAccountCredentials(
            id = id.ifBlank { null },
            password = password.ifBlank { null },
        )

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
            // 빈 목록을 null 로 접지 않는다 — 「전부 지웠다」와 「안 건드렸다」는 다른 지시이고,
            // 후자는 이제 baseline 비교가 판정한다 (#1617).
            processingMethods = processingMethods,
            leaveMessageBlocks = payload.messageBlocks.toLeaveMessageBlocks(),
            credentials = credentials,
            receivers = selectedReceiverIds.map { ReceiverRefPayload(receiverId = it) },
            memorial = null,
        )
    }
}

/**
 * [baseline] 과 값이 같은 필드를 `null` 로 떨어뜨려 요청에서 키째 빠지게 한다 (#1617).
 *
 * **필드를 빼기만 한다 — 채우지 않는다.** 기준이 없으면([baseline] 이 `null`) 손대지 않고 전량을
 * 그대로 돌려준다.
 *
 * 비교는 [normalizedForDiff] 를 거친 값끼리 한다. 서버가 준 원본과 폼을 왕복한 값은 공백·빈 문자열·
 * 정렬에서 어긋날 수 있고, 그 차이를 「사용자가 고쳤다」로 읽으면 안 건드린 필드가 다시 실린다.
 */
private fun AfternoteUpdatePayload.omittingFieldsUnchangedFrom(baseline: AfternoteUpdatePayload?): AfternoteUpdatePayload {
    if (baseline == null) return this
    val current = normalizedForDiff()
    val original = baseline.normalizedForDiff()
    return copy(
        title = title.takeIf { current.title != original.title },
        processingMethods = processingMethods.takeIf { current.processingMethods != original.processingMethods },
        leaveMessageBlocks = leaveMessageBlocks.takeIf { current.leaveMessageBlocks != original.leaveMessageBlocks },
        credentials = credentials.takeIf { current.credentials != original.credentials },
        receivers = receivers.takeIf { current.receivers != original.receivers },
        memorial = memorial.takeIf { current.memorial != original.memorial },
    )
}

/**
 * 「달라졌는가」만 판정하기 위한 정규형 — **전송값이 아니다.**
 *
 * 서버 원본과 폼 왕복본이 뜻은 같은데 표기만 다른 경우를 흡수한다: 앞뒤 공백, 빈 문자열과 `null`,
 * 그리고 뜻이 없는 정렬(수신자는 순서가 의미를 갖지 않는다 — 처리 방법·곡은 사용자가 정한 순서라
 * 정렬하지 않는다).
 */
private fun AfternoteUpdatePayload.normalizedForDiff(): AfternoteUpdatePayload =
    copy(
        title = title?.trim(),
        processingMethods = processingMethods?.map { it.trim() }?.filter { it.isNotEmpty() },
        leaveMessageBlocks =
            leaveMessageBlocks
                ?.map { LeaveMessageBlock(title = it.title?.trim()?.ifEmpty { null }, body = it.body.trim()) }
                ?.filter { it.body.isNotEmpty() },
        credentials =
            credentials
                ?.let { AfternoteAccountCredentials(id = it.id?.ifBlank { null }, password = it.password?.ifBlank { null }) }
                ?.takeIf { it.id != null || it.password != null },
        receivers = receivers?.sortedBy { it.receiverId },
        memorial =
            memorial?.let { media ->
                MemorialWritePayload(
                    memorialPhotoUrl = media.memorialPhotoUrl?.ifBlank { null },
                    songs = media.songs,
                    memorialVideo = media.memorialVideo?.takeIf { !it.videoUrl.isNullOrBlank() },
                )
            },
    )

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
