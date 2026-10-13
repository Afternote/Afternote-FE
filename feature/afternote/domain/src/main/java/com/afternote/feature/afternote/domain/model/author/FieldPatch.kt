package com.afternote.feature.afternote.domain.model.author

/**
 * 부분 갱신에서 한 슬롯의 세 가지 상태를 가르는 값 (#1617).
 *
 * `null` 하나로는 「안 건드렸다」와 「비워 달라」를 구분할 수 없다. 서버가 그 둘을 실제로 다르게
 * 처리하는 자리 — 애프터노트 플레이리스트의 미디어 슬롯 — 에서는 반드시 이 타입으로 말해야 한다.
 *
 * | 상태 | 뜻 | wire |
 * |---|---|---|
 * | [Unchanged] | 사용자가 만지지 않았다 | 키 자체가 나가지 않는다 |
 * | [Set] 에 `null` | 지워 달라 | 키를 달고 JSON `null` 이 나간다 |
 * | [Set] 에 값 | 이 값으로 바꿔 달라 | 값이 나간다 |
 *
 * 서버는 `PlaylistRequestDeserializer` 의 `node.has(...)` 로 키 유무를 먼저 보고,
 * `AfternotePlaylist.update` 가 그 specified 플래그로 「유지」와 「삭제」를 가른다
 * (Afternote-BE `bbff47c`).
 *
 * 컬렉션 슬롯에는 쓰지 않는다 — 곡·수신자·처리 방법은 `null`(안 건드림)과 빈 배열(전부 삭제)로
 * 이미 두 뜻을 다 말할 수 있다. 서버도 `songs != null` 여부만 보고 통째로 갈아 끼운다.
 */
sealed interface FieldPatch<out T> {
    /** 만지지 않았다 — 요청에서 키째 뺀다. */
    data object Unchanged : FieldPatch<Nothing>

    /** 이 값으로 바꾼다. [value] 가 `null` 이면 삭제 지시다. */
    data class Set<out T>(
        val value: T,
    ) : FieldPatch<T>

    companion object {
        /**
         * [current] 가 [baseline] 과 다를 때만 [Set] 으로 싣는다.
         * 안 건드린 슬롯을 실어 남의 변경을 덮는 일을 이 한 줄로 막는다.
         */
        fun <T> changedOrUnchanged(
            current: T,
            baseline: T,
        ): FieldPatch<T> = if (current == baseline) Unchanged else Set(current)
    }
}
