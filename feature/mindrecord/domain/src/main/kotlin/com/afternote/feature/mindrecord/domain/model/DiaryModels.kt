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
    /** 이 일기를 전달받을 수신자 ID 목록. 미선택 시 빈 목록 — 서버가 "수신자 없음" 으로 본다. */
    val receiverIds: List<Long>,
)

data class DiaryUpdatePayload(
    val title: String,
    val content: String,
    val isDraft: Boolean,
    val todayMood: TodayMood,
    val date: String,
    val imageUrl: String? = null,
)
