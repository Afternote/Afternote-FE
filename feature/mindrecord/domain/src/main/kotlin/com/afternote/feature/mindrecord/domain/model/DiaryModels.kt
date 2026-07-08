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
    val date: String? = null,
    val createdAt: String,
    val isDraft: Boolean = false,
    val todayMood: TodayMood,
    val imageUrl: String? = null,
)

data class DiaryList(
    val diaries: List<Diary>,
    val monthDiaryCount: Int,
    val weeklyDominantMood: TodayMood?,
    val receivers: List<MindRecordReceiver> = emptyList(),
)

data class DiaryCreatePayload(
    val title: String,
    val content: String,
    val isDraft: Boolean,
    val todayMood: TodayMood,
    val imageUrl: String? = null,
    val receiverIds: List<Long> = emptyList(),
)

data class DiaryUpdatePayload(
    val title: String? = null,
    val content: String? = null,
    val isDraft: Boolean? = null,
    val todayMood: TodayMood? = null,
    val receiverIds: List<Long> = emptyList(),
)
