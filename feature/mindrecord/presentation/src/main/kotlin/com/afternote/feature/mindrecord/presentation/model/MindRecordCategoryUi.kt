package com.afternote.feature.mindrecord.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val imageRes: Int,
) {
    data object DailyQuestion : MindRecordCategoryUi(
        category = MindRecordCategory.DAILY_QUESTION,
        titleRes = R.string.mindrecord_category_daily_question_title,
        descriptionRes = R.string.mindrecord_category_daily_question_description,
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
        titleRes = R.string.mindrecord_category_diary_title,
        descriptionRes = R.string.mindrecord_category_diary_description,
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

    data object WeeklyReport : MindRecordCategoryUi(
        category = MindRecordCategory.WEEKLY_REPORT,
        titleRes = R.string.mindrecord_category_weekly_report_title,
        descriptionRes = 0,
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
                MindRecordCategory.WEEKLY_REPORT -> WeeklyReport
            }

        fun entries(): List<MindRecordCategoryUi> = listOf(DailyQuestion, Diary, WeeklyReport)
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
