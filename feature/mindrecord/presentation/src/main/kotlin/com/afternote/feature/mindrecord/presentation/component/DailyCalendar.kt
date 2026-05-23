package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.model.DayState
import com.afternote.feature.mindrecord.presentation.model.DayUiModel
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategoryUi
import com.afternote.feature.mindrecord.presentation.R as MindRecordR
import java.util.Calendar

@Composable
fun DailyCalendar(
    year: Int,
    month: Int,
    type: MindRecordCategoryUi,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    answeredDays: Set<Int> = emptySet(),
    emotionByDay: Map<Int, String> = emptyMap(),
) {
    val days = buildDays(year, month, answeredDays, emotionByDay)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.clickable {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.core_ui_arrow_left),
                contentDescription = null,
                modifier = Modifier.clickable { onPrevMonth() },
            )
            Text(
                text = stringResource(MindRecordR.string.mindrecord_calendar_year_month, year, month),
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
            text = stringResource(MindRecordR.string.mindrecord_calendar_answered_count, answeredDays.size),
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.black.copy(alpha = 0.35f),
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
                val dayLabels =
                    listOf(
                        stringResource(MindRecordR.string.mindrecord_calendar_day_label_sun),
                        stringResource(MindRecordR.string.mindrecord_calendar_day_label_mon),
                        stringResource(MindRecordR.string.mindrecord_calendar_day_label_tue),
                        stringResource(MindRecordR.string.mindrecord_calendar_day_label_wed),
                        stringResource(MindRecordR.string.mindrecord_calendar_day_label_thu),
                        stringResource(MindRecordR.string.mindrecord_calendar_day_label_fri),
                        stringResource(MindRecordR.string.mindrecord_calendar_day_label_sat),
                    )

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayLabels.forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            modifier = Modifier.weight(1f),
                            color = AfternoteDesign.colors.black.copy(alpha = 0.3f),
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
    answeredDays: Set<Int>,
    emotionByDay: Map<Int, String>,
): List<DayUiModel> {
    val calendar =
        Calendar.getInstance().apply {
            set(year, month - 1, 1)
        }
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=일
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val now = Calendar.getInstance()
    val today =
        if (now.get(Calendar.YEAR) == year && now.get(Calendar.MONTH) == month - 1) {
            now.get(Calendar.DAY_OF_MONTH)
        } else {
            null
        }

    return buildList {
        // 앞 빈 셀
        repeat(firstDayOfWeek) { add(DayUiModel(day = null)) }
        for (day in 1..daysInMonth) {
            val state =
                when {
                    day == today -> DayState.TODAY
                    day in answeredDays -> DayState.ANSWERED
                    else -> DayState.NONE
                }
            add(
                DayUiModel(
                    day = day,
                    state = state,
                    emotion = emotionByDay[day],
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyCalendarPreview() {
    AfternoteTheme {
        DailyCalendar(
            year = 2026,
            month = 1,
            type = MindRecordCategoryUi.DailyQuestion,
            onNextMonth = {},
            onPrevMonth = {},
            answeredDays = setOf(3, 7, 14),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyCalendarDiaryPreview() {
    AfternoteTheme {
        DailyCalendar(
            year = 2026,
            month = 1,
            type = MindRecordCategoryUi.Diary,
            onNextMonth = {},
            onPrevMonth = {},
            answeredDays = setOf(3, 7, 14),
            emotionByDay = mapOf(3 to "😊", 7 to "😢", 14 to "😐"),
        )
    }
}
