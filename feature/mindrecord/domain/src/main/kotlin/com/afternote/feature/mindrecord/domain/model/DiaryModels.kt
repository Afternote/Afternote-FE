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
    val createdAt: String,
    val todayMood: TodayMood,
    val imageUrl: String? = null,
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
    val imageUrl: String? = null,
    /** 이 일기를 전달받을 수신자 ID 목록. 미선택 시 null (서버 기본 동작). */
    val receiverIds: List<Long>? = null,
)

data class DiaryUpdatePayload(
    val title: String,
    val content: String,
    val isDraft: Boolean,
    val todayMood: TodayMood,
    val imageUrl: String? = null,
    /**
     * 이 일기를 전달받을 수신자 ID 목록. 생성 경로와 같은 형태다 (#955).
     *
     * 서버 규칙: **null 이면 기존 수신자를 그대로 두고, 빈 목록이면 전체 해제**한다 (실측).
     * 그래서 "고른 게 없음" 을 빈 목록으로 보내면 안 된다 — 수신자를 건드리지 않은 편집이
     * 기존 지정을 지워 버린다.
     */
    val receiverIds: List<Long>? = null,
)
