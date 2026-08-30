package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

data class CreateMemorialPayload(
    val title: String,
    val memorial: MemorialWritePayload,
    val leaveMessageBlocks: List<LeaveMessageBlock> = emptyList(),
    val receiverIds: List<Long> = emptyList(),
)

/**
 * 추억 노트 미디어·곡의 **폼 전체 스냅샷** — **생성(POST) 전용**이다.
 * 「지금 이 노트가 가져야 할 상태」를 통째로 말한다.
 *
 * 슬롯에 기본값을 두지 않는 것이 계약이다: 뜻을 적게 해, 일부만 아는 호출부가 나머지를 조용히
 * 비우는 일을 타입에서 막는다. 와이어에서 그 뜻이 어떻게 표현되는지는
 * `AfternotePlaylistRequestDto` 의 KDoc 에 있다 (#1596·#1599).
 *
 * **수정(PATCH)에는 쓰지 않는다.** 수정은 「만진 슬롯만」 말해야 하는데 이 타입은 그것을 표현할
 * 수 없다 — 전체를 말하므로, 곡만 바꾼 저장도 사진·영상을 함께 실어 그 사이 다른 기기가 올린
 * 미디어를 지운다. 수정은 [MemorialPatchPayload] 를 쓴다 (#1617).
 */
data class MemorialWritePayload(
    val memorialPhotoUrl: String?,
    val songs: List<MemorialSongPayload>,
    val memorialVideo: MemorialVideoPayload?,
)

data class MemorialVideoPayload(
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
)

/**
 * 추억 노트 미디어·곡의 **부분 수정** 지시 (#1617).
 *
 * [MemorialWritePayload] 와 결정적으로 다르다 — 저쪽은 「지금 이 노트가 가져야 할 상태」를 통째로
 * 말하고(생성), 이쪽은 「내가 만진 슬롯만」 말한다(수정). 수정에서 전체 스냅샷을 실으면, 에디터를
 * 연 뒤 서버가 바뀐 경우 만진 적 없는 슬롯이 낡은 값으로 덮인다 — 곡만 바꾼 저장이 그 사이 올라온
 * 영정사진·추모 영상을 명시적 `null` 로 지우는 사고가 그것이다.
 *
 * 슬롯마다 표현이 다른 것은 서버 계약이 다르기 때문이다:
 * - [memorialPhotoUrl]·[memorialVideo] 는 **키 유무**로 유지/삭제를 가르므로 [FieldPatch] 가 필요하다.
 * - [songs] 는 서버가 `songs != null` 만 보므로 `null`(안 건드림)과 빈 배열(전부 삭제)로 충분하다.
 */
data class MemorialPatchPayload(
    val memorialPhotoUrl: FieldPatch<String?> = FieldPatch.Unchanged,
    val songs: List<MemorialSongPayload>? = null,
    val memorialVideo: FieldPatch<MemorialVideoPayload?> = FieldPatch.Unchanged,
) {
    /** 어느 슬롯도 만지지 않았으면 `playlist` 키 자체를 내보내지 않아야 한다. */
    val isUnchanged: Boolean
        get() =
            memorialPhotoUrl is FieldPatch.Unchanged &&
                songs == null &&
                memorialVideo is FieldPatch.Unchanged
}

data class MemorialSongPayload(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)
