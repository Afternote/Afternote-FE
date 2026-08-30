package com.afternote.feature.afternote.domain.model.author

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.model.LeaveMessageBlock

/**
 * 애프터노트 수정(PATCH) 페이로드 — **부분 갱신이다.**
 *
 * 서버는 수정 요청을 「받은 키만 반영」으로 읽는다: `AfternoteService.updateAfternote` 가 필드마다
 * `writeRequest.getX() != null` 을 보고 null 이면 기존 엔티티 값을 그대로 남긴다
 * (Afternote-BE `bbff47c` · BE#200·#201). 그래서 **필드를 비워 두는 것이 곧 「이 필드는 건드리지
 * 않았다」는 뜻**이고, 값을 채우는 것이 곧 「이 값으로 갈아 끼워라」다.
 *
 * 세 상태를 구분한다 (#1617):
 *
 * | 표현 | 뜻 | wire |
 * |---|---|---|
 * | `null` | 사용자가 만지지 않았다 | 키 자체가 나가지 않는다 |
 * | 빈 컬렉션 | 전부 지워 달라 | `[]` 가 나간다 |
 * | 값 | 이 값으로 바꿔 달라 | 값이 나간다 |
 *
 * **`null` 을 「빈 값」의 동의어로 쓰면 안 된다.** 폼이 들고 있는 전체 스냅샷을 매번 통째로 실으면,
 * 에디터를 연 뒤 서버가 바뀐 경우 사용자가 만진 적 없는 필드까지 낡은 값으로 덮어쓴다 — 곡을 건드린
 * 적 없는 저장이 남이 추가한 곡을 전부 지우는 lost update 다 (#1617). 그래서 페이로드를 조립하는
 * `AfternoteEditorFormMapper` 는 **프리필 원본(baseline)과 비교해 실제로 달라진 필드만** 채운다.
 *
 * [type] 만 늘 채운다 — 값을 바꾸는 필드가 아니라 **어느 애프터노트를 고치는지 확인하는 단언**이다.
 * 서버는 저장된 카테고리와 다르면 400 을 내고(`CATEGORY_CANNOT_BE_CHANGED`), 같으면 아무것도
 * 바꾸지 않는다. 카테고리는 수정 화면에서 애초에 바꿀 수 없다.
 *
 * 미디어만 규칙이 다르다 — [MemorialWritePayload] 안에서는 빈 배열이 아니라 **명시적 JSON null** 이
 * 삭제다 (#1596). 그쪽 계약은 해당 DTO KDoc 에 적혀 있다.
 */
data class AfternoteUpdatePayload(
    val type: AfternoteType,
    val title: String? = null,
    val processingMethods: List<String>? = null,
    val leaveMessageBlocks: List<LeaveMessageBlock>? = null,
    val credentials: AfternoteAccountCredentials? = null,
    val receivers: List<ReceiverRefPayload>? = null,
    val memorial: MemorialWritePayload? = null,
)

data class AfternoteAccountCredentials(
    val id: String? = null,
    val password: String? = null,
)

data class ReceiverRefPayload(
    val receiverId: Long,
)
