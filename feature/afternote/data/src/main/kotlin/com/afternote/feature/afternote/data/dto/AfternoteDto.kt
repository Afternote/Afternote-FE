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
)

/**
 * 추억 노트 플레이리스트 쓰기 바디 — 생성(POST)·수정(PATCH) 공용.
 *
 * **키가 나가느냐 마느냐가 곧 의미다.** BE 는 수정에서 「키 없음 = 기존 값 유지」와
 * 「키 있고 JSON null = 삭제(DB 참조 제거 + S3 객체 회수)」를 가른다 — 판별은
 * `PlaylistRequestDeserializer` 의 `node.has(...)`, 반영은 `AfternotePlaylist.update` 의 specified
 * 플래그다 (Afternote-BE `72fee63` · BE#259).
 *
 * kotlinx.serialization 은 `encodeDefaults = false`(`NetworkModule.provideJson()` 이 켜지 않으므로
 * 기본값)에서 **기본값과 같은 값을 키째 뺀다.** 그래서 이 클래스에서 기본값의 유무는 편의가 아니라
 * 계약이다 — 기본값을 달아 두면 그 슬롯은 「유지」 말고는 말할 수 없게 된다 (#1596).
 *
 * - [memorialPhotoUrl] · [memorialVideo] 는 기본값을 두지 않는다. 폼이 비었으면 `null` 이 그대로
 *   실려 삭제로 읽힌다. 그러므로 이 DTO 는 **폼 전체 스냅샷에서만** 만들어야 한다 — 일부만 아는
 *   호출부가 만들면 나머지 슬롯이 조용히 지워진다.
 * - [atmosphere] 는 기본값을 남겨 늘 생략한다. FE 화면에 없는 값이라 삭제를 지시할 자격이 없다.
 * - `memorialAudioUrl` 은 아직 필드를 두지 않는다 — 같은 이유이고, 첨부 수단이 생기는 #1118 에서
 *   위 규칙대로 편입한다.
 * - [songs] 는 기본값을 남긴다. 「빈 목록이면 생략」은 #1295 가 못박은 결정이라 이 PR 이 뒤집지
 *   않는다 — 그 결과 「곡 전부 삭제」를 FE 가 표현하지 못하는 것은 별개 결함으로 #1599 이다.
 */
@Serializable
data class AfternotePlaylistRequestDto(
    @SerialName("atmosphere") val atmosphere: String? = null,
    @SerialName("memorialPhotoUrl") val memorialPhotoUrl: String?,
    @SerialName("songs") val songs: List<AfternoteSongDto> = emptyList(),
    @SerialName("memorialVideo") val memorialVideo: AfternoteMemorialVideoDto?,
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
