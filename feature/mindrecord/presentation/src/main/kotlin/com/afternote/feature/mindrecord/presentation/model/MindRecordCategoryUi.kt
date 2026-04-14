package com.afternote.feature.mindrecord.presentation.model

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.afternote.core.model.MindRecordCategory
import com.afternote.feature.mindrecord.presentation.R

sealed class MindRecordCategoryUi(
    val category: MindRecordCategory,
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int,
) {
    data object DailyQuestion : MindRecordCategoryUi(
        category = MindRecordCategory.DAILY_QUESTION,
        title = "데일리 질문",
        description = "매일 다른 질문을 남겨 보세요.",
        imageRes = R.drawable.mindrecord_dailyquestion,
    ) {
        @Composable
        override fun DayIndicator(
            model: DayUiModel,
            textColor: Color,
        ) {
            if (model.state == DayState.ANSWERED) Dot(textColor)
        }
    }

    data object Diary : MindRecordCategoryUi(
        category = MindRecordCategory.DIARY,
        title = "일기",
        description = "나의 매일을 기록하세요",
        imageRes = R.drawable.mindrecord_diary,
    ) {
        @Composable
        override fun DayIndicator(
            model: DayUiModel,
            textColor: Color,
        ) {
            if (model.state == DayState.ANSWERED) {
                model.emotion?.let { Text(it) } ?: Dot(textColor)
            }
        }
    }

    data object DeepThought : MindRecordCategoryUi(
        category = MindRecordCategory.DEEP_THOUGHT,
        title = "깊은 생각",
        description = "오늘의 생각을 남기세요",
        imageRes = R.drawable.mindrecord_deepthought,
    ) {
        @Composable
        override fun DayIndicator(
            model: DayUiModel,
            textColor: Color,
        ) {
            if (model.state == DayState.ANSWERED) Dot(textColor)
        }
    }

    data object WeeklyReport : MindRecordCategoryUi(
        category = MindRecordCategory.WEEKLY_REPORT,
        title = "주간리포트",
        description = "",
        imageRes = 0,
    ) {
        @Composable
        override fun DayIndicator(
            model: DayUiModel,
            textColor: Color,
        ) = Unit
    }

    @Composable
    abstract fun DayIndicator(
        model: DayUiModel,
        textColor: Color,
    )

    companion object {
        fun from(category: MindRecordCategory): MindRecordCategoryUi =
            when (category) {
                MindRecordCategory.DAILY_QUESTION -> DailyQuestion
                MindRecordCategory.DIARY -> Diary
                MindRecordCategory.DEEP_THOUGHT -> DeepThought
                MindRecordCategory.WEEKLY_REPORT -> WeeklyReport
            }

        fun entries(): List<MindRecordCategoryUi> = listOf(DailyQuestion, Diary, DeepThought, WeeklyReport)
    }
}

// 공통 Dot composable — 중복 제거
@Composable
private fun Dot(textColor: Color) {
    Box(
        modifier =
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(textColor),
    )
}
