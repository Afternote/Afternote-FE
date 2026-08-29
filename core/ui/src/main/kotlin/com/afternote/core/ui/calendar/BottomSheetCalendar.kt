package com.afternote.core.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import java.time.LocalDate

data class PickerDayUiModel(
    val day: Int?,
    val isSelected: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetCalendar(
    onDismiss: () -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    title: String,
    initialDate: LocalDate = LocalDate.now(),
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
                        // 근사 토큰: 원본 #DDDDDD → 최근접 gray3(#E0E0E0, 채널당 +3)
                        .background(AfternoteDesign.colors.gray3),
            )
        },
    ) {
        DatePickerContent(
            title = title,
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
            onDateSelect = { day ->
                selectedDate = LocalDate.of(currentYear, currentMonth, day)
                onDateSelect(selectedDate)
            },
        )
    }
}

@Composable
fun DatePickerContent(
    title: String,
    currentYear: Int,
    currentMonth: Int,
    selectedDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val previousMonthContentDescription = stringResource(R.string.core_ui_calendar_previous_month)
    val nextMonthContentDescription = stringResource(R.string.core_ui_calendar_next_month)
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
        Text(
            text = title,
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        // 근사 토큰: 원본 #F5F5F5 → 최근접 gray1(#FAFAFA, 채널당 +5)
                        color = AfternoteDesign.colors.gray1,
                        shape = RoundedCornerShape(12.dp),
                    ).padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text(
                text = formattedDate,
                style = AfternoteDesign.typography.textField,
                color = AfternoteDesign.colors.gray9,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedCard(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, AfternoteDesign.colors.black.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.core_ui_calendar_year_month, currentYear, currentMonth),
                        style = AfternoteDesign.typography.bodySmallR,
                        color = AfternoteDesign.colors.gray9,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier =
                            Modifier
                                .height(8.dp)
                                .width(4.dp)
                                .clipToBounds()
                                .clickable(role = Role.Button) { onPrevMonth() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.core_ui_arrow_left),
                            contentDescription = previousMonthContentDescription,
                            modifier = Modifier.size(24.dp),
                            tint = AfternoteDesign.colors.gray9,
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier =
                            Modifier
                                .height(8.dp)
                                .width(4.dp)
                                .clipToBounds()
                                .clickable(role = Role.Button) { onNextMonth() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.core_ui_right_arrow),
                            contentDescription = nextMonthContentDescription,
                            modifier = Modifier.size(24.dp),
                            tint = AfternoteDesign.colors.gray9,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                CalendarGridContent(
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    onDateSelect = onDateSelect,
                )
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun CalendarGridContent(
    currentYear: Int,
    currentMonth: Int,
    selectedDate: LocalDate,
    onDateSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val dayLabels = stringArrayResource(R.array.core_ui_calendar_day_labels)
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = AfternoteDesign.colors.black.copy(alpha = 0.3f),
                    style = AfternoteDesign.typography.footnoteCaption,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                            onSelect = { dayModel.day?.let(onDateSelect) },
                        )
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PickerDayCell(
    model: PickerDayUiModel,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (model.day == null) {
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    // 근사 토큰: 선택 셀 bg 원본 #1A1A1A → gray9(#212121, 채널당 +7)
    val bgColor = if (model.isSelected) AfternoteDesign.colors.gray9 else Color.Transparent
    // 근사 토큰: 미선택 텍스트 원본 #888888 → gray6(#757575, 채널당 -19). 토큰화 7곳 중 시각 차 가장 큼.
    val textColor = if (model.isSelected) AfternoteDesign.colors.white else AfternoteDesign.colors.gray6

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(4.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(role = Role.RadioButton, onClick = onSelect)
                .semantics { selected = model.isSelected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = model.day.toString(),
            style = AfternoteDesign.typography.captionLargeB,
            color = textColor,
        )
    }
}

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
