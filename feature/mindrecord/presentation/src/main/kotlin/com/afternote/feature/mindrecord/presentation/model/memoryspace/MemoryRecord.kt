package com.afternote.feature.mindrecord.presentation.model.memoryspace

import com.afternote.feature.mindrecord.domain.model.MindRecordType
import java.time.LocalDate

/**
 * 추억 공간 카드가 될 기록 하나 — 두 출처(일기·데일리질문)를 한 줄로 세운 뒤의 모습.
 *
 * 표시용 문자열이 아니라 [date] 를 `LocalDate` 로 든다. 정렬 키가 필요해서다 —
 * "2026.08.23" 같은 표시 문자열로는 두 출처를 섞어 최신순으로 세울 수 없다.
 */
data class MemoryRecord(
    val id: MemoryRecordId,
    val date: LocalDate,
    val title: String,
    val content: String,
    val imageUrl: String?,
    /** 일기의 오늘의 기분. 데일리질문에는 없다. */
    val emotion: String?,
)

/**
 * 추억 공간 안에서 기록 하나를 가리키는 식별자 (#1693).
 *
 * **두 출처의 ID 공간이 겹친다** — 일기 12번과 데일리질문 12번이 동시에 있을 수 있다.
 * 종전에는 데일리질문 ID 를 음수로 접어 갈랐는데(`id = -ui.id`), 그 우회는 두 가지가
 * 나쁘다. 서버 ID 가 양수라는 가정에 기대고, 카드에서 원본으로 되돌아갈 때 부호를 다시
 * 뒤집어야 한다는 지식이 화면마다 필요해진다.
 *
 * 종류를 값으로 들면 그 우회가 사라진다 — 같은 숫자가 와도 [type] 이 다르면 다른 기록이다.
 */
data class MemoryRecordId(
    val type: MindRecordType,
    val value: Long,
)
