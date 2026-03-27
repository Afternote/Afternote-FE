package com.afternote.feature.mindrecord.presentation.model

import com.afternote.feature.mindrecord.presentation.R

enum class MindRecordCategory(
    val title: String,
    val description: String,
    val imageUrl: Int,
) {
    DAILY_QUESTION("데일리 질문", "매일 다른 질문을 남겨 보세요.", R.drawable.mindrecord_dailyquestion),
    DIARY("일기", "나의 매일을 기록하세요", R.drawable.mindrecord_diary),
    DEEP_THOUGHT("깊은 생각", "오늘의 생각을 남기세요", R.drawable.mindrecord_deepthought),
}
