package com.afternote.feature.afternote.data.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AfternoteCreateGalleryRequestDto(
    @EncodeDefault @SerialName("category") val category: String = "GALLERY",
    @SerialName("title") val title: String,
    @SerialName("actions") val processingMethods: List<String>,
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRefDto>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AfternoteCreatePlaylistRequestDto(
    @EncodeDefault @SerialName("category") val category: String = "PLAYLIST",
    @SerialName("title") val title: String,
    @SerialName("playlist") val memorial: AfternotePlaylistRequestDto,
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRefDto> = emptyList(),
)

/** SOCIAL·BUSINESS 공용 생성 요청 — 두 카테고리는 바디 스키마가 동일해 [category] 값으로만 구분된다. */
@Serializable
data class AfternoteCreateAccountRequestDto(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("actions") val processingMethods: List<String>,
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("credentials") val credentials: AfternoteCredentialsDto? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRefDto> = emptyList(),
)

@Serializable
data class AfternoteUpdateRequestDto(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("actions") val processingMethods: List<String>? = null,
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("credentials") val credentials: AfternoteCredentialsDto? = null,
    @SerialName("receivers") val receivers: List<AfternoteReceiverRefDto>? = null,
    @SerialName("playlist") val memorial: AfternotePlaylistRequestDto? = null,
)

@Serializable
data class AfternoteDetailDto(
    @SerialName("afternoteId") val afternoteId: Long,
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("isDraft") val isDraft: Boolean,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("credentials") val credentials: AfternoteCredentialsDto? = null,
    @SerialName("receivers") val receivers: List<AfternoteDetailReceiverDto>,
    @SerialName("actions") val processingMethods: List<String>? = null,
    @SerialName("leaveMessage") val leaveMessage: List<LeaveMessageBlockDto>? = null,
    @SerialName("playlist") val memorial: AfternotePlaylistDto? = null,
)

@Serializable
data class AfternotePlaylistDto(
    @SerialName("atmosphere") val atmosphere: String? = null,
    @SerialName("memorialPhotoUrl") val memorialPhotoUrl: String? = null,
    @SerialName("songs") val songs: List<AfternoteSongDto>,
    @SerialName("memorialVideo") val memorialVideo: AfternoteMemorialVideoDto? = null,
    // 추모 음성 (#1118). 서버 응답에서 nullable — 미첨부면 null 로 온다.
    @SerialName("memorialAudioUrl") val memorialAudioUrl: String? = null,
)

@Serializable
data class AfternotePlaylistRequestDto(
    @SerialName("atmosphere") val atmosphere: String? = null,
    @SerialName("memorialPhotoUrl") val memorialPhotoUrl: String? = null,
    @SerialName("songs") val songs: List<AfternoteSongDto> = emptyList(),
    @SerialName("memorialVideo") val memorialVideo: AfternoteMemorialVideoDto? = null,
    /**
     * 추모 음성 URL (#1118). 업로드로 발급된 afternotes 키(mp3·m4a·wav)만 서버가 받는다.
     *
     * **기본값을 두지 않는 것이 계약이다.** kotlinx.serialization 은 `encodeDefaults = false`
     * (`NetworkModule.provideJson()` 이 켜지 않으므로 기본값)에서 기본값과 같은 값을 키째 뺀다.
     * BE 는 수정에서 「키 없음 = 기존 값 유지」와 「키 있고 JSON null = 삭제」를 가르므로
     * (`PlaylistRequestDeserializer` 의 `node.has(...)` → `AfternotePlaylist.update` 의 specified 플래그,
     * Afternote-BE `72fee63`), 기본값을 달면 이 슬롯은 「유지」 말고는 말할 수 없게 된다.
     * 같은 규칙을 사진·영상까지 넓히는 것은 #1596 이 맡는다.
     *
     * 기본값이 없으므로 이 DTO 는 **폼 전체 스냅샷에서만** 만들어야 한다 — 음성을 모르는 호출부가
     * 만들면 서버 음성이 조용히 지워진다.
     */
    @SerialName("memorialAudioUrl") val memorialAudioUrl: String?,
)

@Serializable
data class AfternoteSongDto(
    @SerialName("title") val title: String,
    @SerialName("artist") val artist: String,
    @SerialName("coverUrl") val coverUrl: String? = null,
)

@Serializable
data class AfternoteMemorialVideoDto(
    @SerialName("videoUrl") val videoUrl: String? = null,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
)

@Serializable
data class AfternoteDetailReceiverDto(
    @SerialName("receiverId") val receiverId: Long? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("relation") val relation: String? = null,
)

@Serializable
data class AfternoteListItemDto(
    @SerialName("afternoteId") val afternoteId: Long,
    @SerialName("title") val title: String,
    @SerialName("category") val category: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("isDraft") val isDraft: Boolean,
)

@Serializable
data class AfternoteIdDto(
    @SerialName("afternoteId") val afternoteId: Long,
)

@Serializable
data class AfternotePageDto(
    @SerialName("content") val content: List<AfternoteListItemDto>,
    @SerialName("page") val page: Int,
    @SerialName("size") val size: Int,
    @SerialName("hasNext") val hasNext: Boolean,
)

@Serializable
data class AfternoteCredentialsDto(
    @SerialName("id") val id: String? = null,
    @SerialName("password") val password: String? = null,
)

@Serializable
data class AfternoteReceiverRefDto(
    @SerialName("receiverId") val receiverId: Long,
)
