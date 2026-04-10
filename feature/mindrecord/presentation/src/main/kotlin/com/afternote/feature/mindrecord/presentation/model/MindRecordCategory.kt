package com.afternote.feature.mindrecord.presentation.model

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
import com.afternote.feature.mindrecord.presentation.R

enum class MindRecordCategory(
    val title: String,
    val description: String,
    val imageUrl: Int,
) {
    DAILY_QUESTION("데일리 질문", "매일 다른 질문을 남겨 보세요.", R.drawable.mindrecord_dailyquestion) {
        @Composable
        override fun DayIndicator(
            model: DayUiModel,
            textColor: Color,
        ) {
            if (model.state == DayState.ANSWERED) {
                Box(
                    modifier =
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(textColor),
                )
            }
        }
    },
    DIARY("일기", "나의 매일을 기록하세요", R.drawable.mindrecord_diary) {
        @Composable
        override fun DayIndicator(
            model: DayUiModel,
            textColor: Color,
        ) {
            if (model.state == DayState.ANSWERED) {
                if (model.emotion != null) {
                    Text(text = model.emotion)
                } else {
                    Box(
                        modifier =
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(textColor),
                    )
                }
            }
        }
    },
    DEEP_THOUGHT("깊은 생각", "오늘의 생각을 남기세요", R.drawable.mindrecord_deepthought) {
        @Composable
        override fun DayIndicator(
            model: DayUiModel,
            textColor: Color,
        ) {
            if (model.state == DayState.ANSWERED) {
                Box(
                    modifier =
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(textColor),
                )
            }
        }
    },

    WEEKLY_REPORT("주간리포트", "주간리포트", R.drawable.mindrecord_picture) {
        @Composable
        override fun DayIndicator(
            model: DayUiModel,
            textColor: Color,
        ) {
        }
    }, ;

    @Composable
    abstract fun DayIndicator(
        model: DayUiModel,
        textColor: Color,
    )
}
