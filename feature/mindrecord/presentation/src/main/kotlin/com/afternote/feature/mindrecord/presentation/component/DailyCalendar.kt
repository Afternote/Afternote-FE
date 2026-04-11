package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.model.DayState
import com.afternote.feature.mindrecord.presentation.model.DayUiModel
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory
import java.util.Calendar

@Composable
fun DailyCalendar(
    year: Int,
    month: Int,
    type: MindRecordCategory,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = buildDays(year, month)

    Column {
        Row(
            modifier = modifier.clickable {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.core_ui_arrow_left),
                contentDescription = null,
                modifier = Modifier.clickable { onPrevMonth() },
            )
            Text(
                text = "${year}년 ${month}월",
                color = AfternoteDesign.colors.gray9,
                style = AfternoteDesign.typography.h3,
            )
            Icon(
                painter = painterResource(R.drawable.core_ui_right),
                contentDescription = null,
                modifier = Modifier.clickable { onNextMonth() },
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "18개의 답변 완료",
            style = AfternoteDesign.typography.captionLargeR,
            color = Color(0xFF000000).copy(alpha = 0.35f),
        )
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedCard(
            colors =
                CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFFFF),
                ),
            border = BorderStroke(1.dp, color = Color(0xFF000000).copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayLabels.forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF000000).copy(alpha = 0.3f),
                            style = AfternoteDesign.typography.footnoteCaption,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                val chunked = days.chunked(7)

                Column {
                    chunked.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { dayModel ->
                                Box(modifier = Modifier.weight(1f)) {
                                    DayCell(model = dayModel, type = type)
                                }
                            }
                            // 마지막 주가 7개 미만이면 빈 셀로 채우기
                            repeat(7 - week.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

fun buildDays(
    year: Int,
    month: Int,
): List<DayUiModel> {
    val calendar =
        Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=일
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today =
        Calendar
            .getInstance()
            .get(Calendar.DAY_OF_MONTH)
            .takeIf { Calendar.getInstance().get(Calendar.MONTH) == month - 1 }

    return buildList {
        // 앞 빈 셀
        repeat(firstDayOfWeek) { add(DayUiModel(day = null)) }
        // 날짜 셀 (실제 상태는 ViewModel에서 주입)
        for (day in 1..daysInMonth) {
            val state =
                when (day) {
                    today -> DayState.TODAY
                    else -> DayState.ANSWERED // ViewModel에서 실제 데이터로 교체
                }
            add(DayUiModel(day = day, state = state))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyCalendarPreview() {
    AfternoteTheme {
        DailyCalendar(
            year = 2022,
            month = 1,
            type = MindRecordCategory.DAILY_QUESTION,
            onNextMonth = {},
            onPrevMonth = {},
        )
    }
}
