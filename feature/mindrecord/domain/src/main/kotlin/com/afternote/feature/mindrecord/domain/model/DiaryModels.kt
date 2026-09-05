package com.afternote.feature.mindrecord.domain.model

enum class TodayMood {
    HAPPY,
    SOSO,
    SAD,
}

data class Diary(
    val diaryId: Long,
    val title: String,
    val content: String,
    /** 사용자가 고른 일기 날짜 (yyyy-MM-dd). 서버가 항상 채우는 값이다 (#789). */
    val date: String,
    val createdAt: String,
    /** 사용자가 직접 고른 오늘의 기분. 저장 컬럼이 필수라 응답에도 항상 있다 (#789). */
    val todayMood: TodayMood,
    /**
     * 목록 카드 썸네일. **서버 응답에 이 키가 없어 항상 null 이다** — 표시 단계에서 본문
     * HTML 의 첫 img 로 채운다 (#1024). 계약이 넓어지면 그때 서버 값이 우선한다.
     */
    val imageUrl: String? = null,
    val isDraft: Boolean = false,
    /** 이 기록을 전달받을 수신자 이름들. 상세 화면이 "수신인 OOO" 로 보여준다 (#759). */
    val receiverNames: List<String> = emptyList(),
)

data class DiaryList(
    val diaries: List<Diary>,
    val monthDiaryCount: Int,
    val weeklyDominantMood: TodayMood?,
)

data class DiaryCreatePayload(
    val title: String,
    val content: String,
    val isDraft: Boolean,
    val todayMood: TodayMood,
    /** 이 일기를 전달받을 수신자 ID 목록. 미선택 시 빈 목록 — 서버가 "수신자 없음" 으로 본다. */
    val receiverIds: List<Long>,
)

data class DiaryUpdatePayload(
    val title: String,
    val content: String,
    val isDraft: Boolean,
    val todayMood: TodayMood,
    /**
     * 이 일기를 전달받을 수신자 ID 목록. 생성 경로와 같은 형태다 (#955).
     *
     * 서버 규칙: **null 이면 기존 수신자를 그대로 두고, 빈 목록이면 전체 해제**한다 (실측).
     * 그래서 "고른 게 없음" 을 빈 목록으로 보내면 안 된다 — 수신자를 건드리지 않은 편집이
     * 기존 지정을 지워 버린다.
     */
    val receiverIds: List<Long>? = null,
)
