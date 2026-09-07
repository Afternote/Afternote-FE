package com.afternote.feature.mindrecord.presentation.model.memoryspace

/**
 * 추억 공간(MEMORY SPACE) 카드 1장.
 *
 * 서버에 전용 계약이 없어 사용자의 실제 기록(일기·데일리질문 답변)에서 조립한다.
 *
 * @param id 원본 기록 식별자. 두 출처의 숫자 ID 공간이 겹치므로 종류를 함께 든다
 *   ([MemoryRecordId], #1693).
 * @param imageUrl 기록에 첨부된 대표 이미지. 없는 기록도 있어 nullable — 카드는 플레이스홀더로 렌더한다.
 * @param content 원본 HTML 본문. 표시 시점에 `htmlToPlainText()` 로 변환한다.
 */
data class MemoryItem(
    val id: MemoryRecordId,
    val imageUrl: String?,
    val title: String,
    val date: String,
    val content: String,
    val tags: List<String>,
)
