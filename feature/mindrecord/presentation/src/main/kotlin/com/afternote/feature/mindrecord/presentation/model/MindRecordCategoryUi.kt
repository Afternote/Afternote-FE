package com.afternote.feature.mindrecord.presentation.model

import com.afternote.core.model.MindRecordCategory
import com.afternote.feature.mindrecord.presentation.R

/** UI 표시용 제목. */
val MindRecordCategory.title: String
    get() =
        when (this) {
            MindRecordCategory.DAILY_QUESTION -> "데일리 질문"
            MindRecordCategory.DIARY -> "일기"
            MindRecordCategory.DEEP_THOUGHT -> "깊은 생각"
        }

/** UI 표시용 설명. */
val MindRecordCategory.description: String
    get() =
        when (this) {
            MindRecordCategory.DAILY_QUESTION -> "매일 다른 질문을 남겨 보세요."
            MindRecordCategory.DIARY -> "나의 매일을 기록하세요"
            MindRecordCategory.DEEP_THOUGHT -> "오늘의 생각을 남기세요"
        }

/** UI 표시용 아이콘 drawable 리소스. */
val MindRecordCategory.imageUrl: Int
    get() =
        when (this) {
            MindRecordCategory.DAILY_QUESTION -> R.drawable.mindrecord_dailyquestion
            MindRecordCategory.DIARY -> R.drawable.mindrecord_diary
            MindRecordCategory.DEEP_THOUGHT -> R.drawable.mindrecord_deepthought
        }
