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
    val date: String,
    val imageUrl: String? = null,
)
