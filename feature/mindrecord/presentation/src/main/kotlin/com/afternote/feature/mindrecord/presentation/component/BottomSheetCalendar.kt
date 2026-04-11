package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.afternote.feature.mindrecord.presentation.model.PickerDayUiModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetCalendar(
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    var currentYear by remember { mutableIntStateOf(initialDate.year) }
    var currentMonth by remember { mutableIntStateOf(initialDate.monthValue) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        modifier = Modifier.fillMaxHeight(0.85f),
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDDDDDD)),
            )
        },
    ) {
        DatePickerContent(
            currentYear = currentYear,
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            onPrevMonth = {
                if (currentMonth == 1) {
                    currentMonth = 12
                    currentYear--
                } else {
                    currentMonth--
                }
            },
            onNextMonth = {
                if (currentMonth == 12) {
                    currentMonth = 1
                    currentYear++
                } else {
                    currentMonth++
                }
            },
            onDaySelected = { day ->
                selectedDate = LocalDate.of(currentYear, currentMonth, day)
                onDateSelected(selectedDate)
            },
        )
    }
}

@Composable
fun DatePickerContent(
    currentYear: Int,
    currentMonth: Int,
    selectedDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate =
        "%d.%02d.%02d".format(
            selectedDate.year,
            selectedDate.monthValue,
            selectedDate.dayOfMonth,
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
    ) {
        // 제목
        Text(
            text = "발송 날짜",
            style = AfternoteDesign.typography.h3,
            color = AfternoteDesign.colors.gray9,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 선택된 날짜 표시 필드
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp),
                    ).padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text(
                text = formattedDate,
                style = AfternoteDesign.typography.h3,
                color = AfternoteDesign.colors.gray9,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 캘린더 카드
        OutlinedCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFF000000).copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                // 헤더: 년월 + 화살표
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${currentYear}년 ${currentMonth}월",
                        style = AfternoteDesign.typography.h3,
                        color = AfternoteDesign.colors.gray9,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        painter = painterResource(R.drawable.core_ui_arrow_left),
                        contentDescription = "이전 달",
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clickable { onPrevMonth() },
                        tint = AfternoteDesign.colors.gray9,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.core_ui_right),
                        contentDescription = "다음 달",
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clickable { onNextMonth() },
                        tint = AfternoteDesign.colors.gray9,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 요일 레이블
                val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF000000).copy(alpha = 0.3f),
                            style = AfternoteDesign.typography.footnoteCaption,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 날짜 그리드
                val days =
                    buildPickerDays(
                        year = currentYear,
                        month = currentMonth,
                        selectedDate = selectedDate,
                    )
                days.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { dayModel ->
                            Box(modifier = Modifier.weight(1f)) {
                                PickerDayCell(
                                    model = dayModel,
                                    onSelected = { dayModel.day?.let(onDaySelected) },
                                )
                            }
                        }
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(50.dp))

        }
    }
}

@Composable
fun PickerDayCell(
    model: PickerDayUiModel,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (model.day == null) {
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    val bgColor = if (model.isSelected) Color(0xFF1A1A1A) else Color.Transparent
    val textColor = if (model.isSelected) Color.White else Color(0xFF888888)

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(4.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(enabled = true, onClick = onSelected),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = model.day.toString(),
            style = AfternoteDesign.typography.captionLargeB,
            color = textColor,
        )
    }
}

// ── Builder ────────────────────────────────────────────────────────────────

fun buildPickerDays(
    year: Int,
    month: Int,
    selectedDate: LocalDate,
): List<PickerDayUiModel> {
    val cal =
        java.util.Calendar
            .getInstance()
            .apply { set(year, month - 1, 1) }
    val firstDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

    return buildList {
        repeat(firstDayOfWeek) { add(PickerDayUiModel(day = null)) }
        for (day in 1..daysInMonth) {
            val isSelected =
                selectedDate.year == year &&
                    selectedDate.monthValue == month &&
                    selectedDate.dayOfMonth == day
            add(PickerDayUiModel(day = day, isSelected = isSelected))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DatePickerContentPreview() {
    AfternoteTheme {
        var selectedDate by remember { mutableStateOf(LocalDate.of(2026, 12, 16)) }
        var year by remember { mutableIntStateOf(2026) }
        var month by remember { mutableIntStateOf(12) }

        DatePickerContent(
            currentYear = year,
            currentMonth = month,
            selectedDate = selectedDate,
            onPrevMonth = {
                if (month == 1) {
                    month = 12
                    year--
                } else {
                    month--
                }
            },
            onNextMonth = {
                if (month == 12) {
                    month = 1
                    year++
                } else {
                    month++
                }
            },
            onDaySelected = { day -> selectedDate = LocalDate.of(year, month, day) },
        )
    }
}
