package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

data class CreateMemorialPayload(
    val title: String,
    val memorial: MemorialWritePayload,
    val leaveMessageBlocks: List<LeaveMessageBlock> = emptyList(),
    val receiverIds: List<Long> = emptyList(),
)

/**
 * 추억 노트 미디어·곡의 **폼 전체 스냅샷**. 「지금 이 노트가 가져야 할 상태」를 통째로 말한다.
 *
 * 수정 저장에서 미디어의 `null` 은 「모르겠다」가 아니라 「비어 있다 — 서버에 있으면 지워라」로
 * 나간다. 그래서 기본값을 두지 않는다: 슬롯마다 뜻을 적게 해, 일부만 아는 호출부가 나머지를
 * 조용히 지우는 일을 타입에서 막는다. 와이어에서 그 뜻이 어떻게 표현되는지는
 * `AfternotePlaylistRequestDto` 의 KDoc 에 있다 (#1596).
 *
 * [songs] 도 같은 뜻이다 — 빈 목록은 「모르겠다」가 아니라 「곡이 하나도 없다」로 나가 서버 곡을
 * 전부 지운다. 와이어 표현만 다르다: 미디어는 JSON `null`, 곡은 **빈 배열**이다 (#1599).
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

data class MemorialSongPayload(
    val title: String,
    val artist: String,
    val coverUrl: String?,
)
