package com.afternote.core.domain.model

data class DailyQuestionAnswerItem(
    val dailyQuestionAnswerId: Long,
    val question: String,
    val answer: String,
    val recordDate: String,
)

data class ReceiverDailyQuestionsResult(
    val items: List<DailyQuestionAnswerItem>,
    val hasNext: Boolean,
)
