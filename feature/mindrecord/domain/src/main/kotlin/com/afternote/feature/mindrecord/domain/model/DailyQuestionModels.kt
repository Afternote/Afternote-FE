package com.afternote.feature.mindrecord.domain.model

data class DailyQuestion(
    val dailyQuestionId: Long,
    val title: String,
    val content: String,
    val createdAt: String,
    val imageUrl: String? = null,
)

data class TodayDailyQuestion(
    val questionId: Long,
    val content: String,
    val isAnswered: Boolean,
)

data class DailyQuestionCreatePayload(
    val content: String,
    val isDraft: Boolean,
    val questionId: Long,
    val imageUrl: String? = null,
)

data class DailyQuestionUpdatePayload(
    val content: String? = null,
    val isDraft: Boolean? = null,
    val date: String? = null,
    val questionId: Long? = null,
    val imageUrl: String? = null,
)
