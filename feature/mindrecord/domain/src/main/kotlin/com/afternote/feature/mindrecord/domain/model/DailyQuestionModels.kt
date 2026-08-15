package com.afternote.feature.mindrecord.domain.model

data class DailyQuestion(
    val dailyQuestionId: Long,
    val title: String,
    val content: String,
    val createdAt: String,
    val imageUrl: String? = null,
    val isDraft: Boolean = false,
)

data class TodayDailyQuestion(
    val questionId: Long,
    /** 서비스 시작일 기준 몇 번째 질문인지 ("Day N" 표기용). 서버가 항상 채우는 값이다 (#789). */
    val day: Int,
    val content: String,
    val isAnswered: Boolean,
    val isDraft: Boolean = false,
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
