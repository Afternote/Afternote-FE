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
    /** 서버가 기분을 주지 않았거나 클라가 모르는 값이면 null — 목록 표시에서 이모지만 생략한다. */
    val todayMood: TodayMood?,
    val imageUrl: String? = null,
    val isDraft: Boolean = false,
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
    val date: String,
    val imageUrl: String? = null,
)
