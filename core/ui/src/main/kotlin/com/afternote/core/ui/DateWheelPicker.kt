package com.afternote.core.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.sd.lib.compose.wheel_picker.FVerticalWheelPicker
import com.sd.lib.compose.wheel_picker.FWheelPickerState
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate

private val PickerContainerHeight = 152.dp
private val SelectionBorderHeight = 40.dp
private val SelectionBorderHorizontalInset = 4.dp
private val DividerWidth = 24.dp

private data class DateWheelPickerColors(
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
    val selectionBorderColor: Color,
    val dividerColor: Color,
)

/**
 * Default values for [DateWheelPicker].
 */
object DateWheelPickerDefaults {
    val ContainerWidth = 228.dp
    val SelectedTextColor = Color(0xFF212121)
    val UnselectedTextColor = Color(0xFFBDBDBD)
    val SelectionBorderColor = Color(0xFF6B8FF8)
    val DividerColor = Color(0xFFE0E0E0)

    /** 기본 연도 범위: 올해 ~ 올해+10년. [DateWheelPicker]의 [yearRange] 기본값. */
    val DefaultYearRange: IntRange
        get() {
            val y = LocalDate.now().year
            return y..(y + 10)
        }
}

/**
 * 발송 날짜 Wheel Picker
 *
 * @param modifier Modifier
 * @param currentDate 현재 선택된 날짜 (State Hoisting)
 * @param onDateChange 날짜 변경 콜백 (현재시제 네이밍 규칙 준수)
 * @param minDate 최소 선택 가능 날짜. null이면 제한 없음(과거·미래 모두 선택 가능).
 * @param yearRange 연도 휠에 표시할 연도 구간. [minDate]·기획에 맞게 호출부에서 넘깁니다.
 * @param selectedTextColor 선택된 텍스트 색상
 * @param unselectedTextColor 선택되지 않은 텍스트 색상
 * @param selectionBorderColor 선택 영역 테두리 색상
 * @param dividerColor 구분선 색상
 */
@Composable
fun DateWheelPicker(
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    currentDate: LocalDate = LocalDate.now(),
    minDate: LocalDate? = null,
    yearRange: IntRange = DateWheelPickerDefaults.DefaultYearRange,
    selectedTextColor: Color = DateWheelPickerDefaults.SelectedTextColor,
    unselectedTextColor: Color = DateWheelPickerDefaults.UnselectedTextColor,
    selectionBorderColor: Color = DateWheelPickerDefaults.SelectionBorderColor,
    dividerColor: Color = DateWheelPickerDefaults.DividerColor,
) {
    val colors =
        DateWheelPickerColors(
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            selectionBorderColor = selectionBorderColor,
            dividerColor = dividerColor,
        )

    val model =
        rememberDateWheelPickerModel(
            currentDate = currentDate,
            minDate = minDate,
            yearRange = yearRange,
        )
    val yearState = rememberFWheelPickerState(initialIndex = model.yearIndex)
    val monthState = rememberFWheelPickerState(initialIndex = model.monthIndex)

    SyncWheelIndex(state = yearState, targetIndex = model.yearIndex)
    SyncWheelIndex(state = monthState, targetIndex = model.monthIndex)

    ObserveYearWheel(
        state = yearState,
        years = model.years,
        currentYearFallback = model.currentYear,
        currentDate = currentDate,
        minDate = minDate,
        onDateChange = onDateChange,
    )
    ObserveMonthWheel(
        state = monthState,
        months = model.months,
        currentDate = currentDate,
        minDate = minDate,
        onDateChange = onDateChange,
    )

    DateWheelPickerContent(
        model = model,
        currentDate = currentDate,
        yearState = yearState,
        monthState = monthState,
        minDate = minDate,
        onDateChange = onDateChange,
        colors = colors,
        modifier = modifier,
    )
}

private data class DateWheelPickerModel(
    val currentYear: Int,
    val years: List<Int>,
    val months: List<Int>,
    val daysInMonth: Int,
    val days: List<Int>,
    val yearIndex: Int,
    val monthIndex: Int,
    val dayIndex: Int,
    val dateDescription: String,
)

@Composable
private fun rememberDateWheelPickerModel(
    currentDate: LocalDate,
    minDate: LocalDate?,
    yearRange: IntRange,
): DateWheelPickerModel {
    val currentYear = LocalDate.now().year
    val years = remember(yearRange.first, yearRange.last) { yearRange.toList() }
    val months = remember { (1..12).toList() }

    val effectiveDate =
        remember(currentDate, minDate) {
            if (minDate != null) currentDate.coerceAtLeast(minDate) else currentDate
        }

    val yearIndex =
        remember(effectiveDate.year, years) {
            years.indexOf(effectiveDate.year).coerceIn(0, years.lastIndex)
        }
    val monthIndex =
        remember(effectiveDate.monthValue, months) {
            (effectiveDate.monthValue - 1).coerceIn(0, months.lastIndex)
        }

    val daysInMonth =
        remember(effectiveDate.year, effectiveDate.monthValue) {
            LocalDate.of(effectiveDate.year, effectiveDate.monthValue, 1).lengthOfMonth()
        }
    val days =
        remember(daysInMonth, minDate, effectiveDate.year, effectiveDate.monthValue) {
            if (minDate != null &&
                effectiveDate.year == minDate.year &&
                effectiveDate.monthValue == minDate.monthValue
            ) {
                (minDate.dayOfMonth..daysInMonth).toList()
            } else {
                (1..daysInMonth).toList()
            }
        }
    val dayIndex =
        remember(effectiveDate.dayOfMonth, days) {
            val idx = days.indexOf(effectiveDate.dayOfMonth)
            idx.coerceIn(0, days.lastIndex)
        }

    val dateDescription =
        "${effectiveDate.year}년 ${effectiveDate.monthValue}월 ${effectiveDate.dayOfMonth}일 선택됨"

    return DateWheelPickerModel(
        currentYear = currentYear,
        years = years,
        months = months,
        daysInMonth = daysInMonth,
        days = days,
        yearIndex = yearIndex,
        monthIndex = monthIndex,
        dayIndex = dayIndex,
        dateDescription = dateDescription,
    )
}

@Composable
private fun DateWheelPickerContent(
    model: DateWheelPickerModel,
    currentDate: LocalDate,
    yearState: FWheelPickerState,
    monthState: FWheelPickerState,
    minDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    colors: DateWheelPickerColors,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.semantics {
                contentDescription = model.dateDescription
            },
    ) {
        // 테두리는 뒤에 그려서 휠 터치가 가려지지 않도록 함
        SelectionBorder(
            selectionBorderColor = colors.selectionBorderColor,
            modifier = Modifier.align(Alignment.Center),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(PickerContainerHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FVerticalWheelPicker(
                count = model.years.size,
                state = yearState,
                modifier = Modifier.weight(4f),
                itemHeight = SelectionBorderHeight,
                unfocusedCount = 1,
                focus = {},
            ) { index ->
                PickerText(
                    text = "${model.years[index]}",
                    isSelected = index == yearState.currentIndex,
                    selectedTextColor = colors.selectedTextColor,
                    unselectedTextColor = colors.unselectedTextColor,
                )
            }

            Divider(
                color = colors.dividerColor,
                modifier = Modifier.width(DividerWidth),
            )

            FVerticalWheelPicker(
                count = model.months.size,
                state = monthState,
                modifier = Modifier.weight(3f),
                itemHeight = SelectionBorderHeight,
                unfocusedCount = 1,
                focus = {},
            ) { index ->
                PickerText(
                    text = "${model.months[index]}",
                    isSelected = index == monthState.currentIndex,
                    selectedTextColor = colors.selectedTextColor,
                    unselectedTextColor = colors.unselectedTextColor,
                )
            }

            Divider(
                color = colors.dividerColor,
                modifier = Modifier.width(DividerWidth),
            )

            DayWheel(
                model =
                    DateWheelPickerDayModel(
                        daysInMonth = model.daysInMonth,
                        days = model.days,
                        dayIndex = model.dayIndex,
                    ),
                currentDate = currentDate,
                minDate = minDate,
                onDateChange = onDateChange,
                colors = colors,
                modifier = Modifier.weight(3f),
            )
        }
    }
}

private data class DateWheelPickerDayModel(
    val daysInMonth: Int,
    val days: List<Int>,
    val dayIndex: Int,
)

@Composable
private fun SelectionBorder(
    selectionBorderColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = SelectionBorderHorizontalInset),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(SelectionBorderHeight)
                    .border(1.dp, selectionBorderColor, RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun DayWheel(
    model: DateWheelPickerDayModel,
    currentDate: LocalDate,
    minDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    colors: DateWheelPickerColors,
    modifier: Modifier = Modifier,
) {
    val dayState = rememberFWheelPickerState(initialIndex = model.dayIndex)

    SyncWheelIndex(state = dayState, targetIndex = model.dayIndex)
    ObserveDayWheel(
        state = dayState,
        days = model.days,
        currentDate = currentDate,
        minDate = minDate,
        onDateChange = onDateChange,
    )

    FVerticalWheelPicker(
        count = model.days.size,
        state = dayState,
        modifier = modifier,
        itemHeight = SelectionBorderHeight,
        unfocusedCount = 1,
        focus = {},
    ) { index ->
        PickerText(
            text = "${model.days[index]}",
            isSelected = index == dayState.currentIndex,
            selectedTextColor = colors.selectedTextColor,
            unselectedTextColor = colors.unselectedTextColor,
        )
    }
}

@Composable
private fun SyncWheelIndex(
    state: FWheelPickerState,
    targetIndex: Int,
) {
    LaunchedEffect(targetIndex) {
        if (state.currentIndex != targetIndex) {
            state.scrollToIndex(targetIndex)
        }
    }
}

@Composable
private fun ObserveYearWheel(
    state: FWheelPickerState,
    years: List<Int>,
    currentYearFallback: Int,
    currentDate: LocalDate,
    minDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
) {
    val currentOnDateChange = rememberUpdatedState(onDateChange)
    LaunchedEffect(state, currentDate, minDate) {
        snapshotFlow { state.currentIndex }
            .distinctUntilChanged()
            .collect { index ->
                val newYear = years.getOrElse(index) { currentYearFallback }
                var newDate = currentDate.withYearClamped(newYear)
                if (minDate != null) newDate = newDate.coerceAtLeast(minDate)
                newDate.takeIf { it != currentDate }?.let(currentOnDateChange.value)
            }
    }
}

@Composable
private fun ObserveMonthWheel(
    state: FWheelPickerState,
    months: List<Int>,
    currentDate: LocalDate,
    minDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
) {
    val currentOnDateChange = rememberUpdatedState(onDateChange)
    LaunchedEffect(state, currentDate, minDate) {
        snapshotFlow { state.currentIndex }
            .distinctUntilChanged()
            .collect { index ->
                val newMonth = months.getOrElse(index) { 1 }
                var newDate = currentDate.withMonthClamped(newMonth)
                if (minDate != null) newDate = newDate.coerceAtLeast(minDate)
                newDate.takeIf { it != currentDate }?.let(currentOnDateChange.value)
            }
    }
}

@Composable
private fun ObserveDayWheel(
    state: FWheelPickerState,
    days: List<Int>,
    currentDate: LocalDate,
    minDate: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
) {
    val currentOnDateChange = rememberUpdatedState(onDateChange)
    LaunchedEffect(state, currentDate, minDate) {
        snapshotFlow { state.currentIndex }
            .distinctUntilChanged()
            .collect { index ->
                val newDay = days.getOrElse(index) { 1 }
                var newDate = currentDate.withDayClamped(newDay)
                if (minDate != null) newDate = newDate.coerceAtLeast(minDate)
                newDate.takeIf { it != currentDate }?.let(currentOnDateChange.value)
            }
    }
}

private fun LocalDate.withYearClamped(newYear: Int): LocalDate {
    val daysInMonth = LocalDate.of(newYear, monthValue, 1).lengthOfMonth()
    val newDay = dayOfMonth.coerceAtMost(daysInMonth)
    return LocalDate.of(newYear, monthValue, newDay)
}

private fun LocalDate.withMonthClamped(newMonth: Int): LocalDate {
    val safeMonth = newMonth.coerceIn(1, 12)
    val daysInMonth = LocalDate.of(year, safeMonth, 1).lengthOfMonth()
    val newDay = dayOfMonth.coerceAtMost(daysInMonth)
    return LocalDate.of(year, safeMonth, newDay)
}

private fun LocalDate.withDayClamped(newDay: Int): LocalDate {
    val daysInMonth = LocalDate.of(year, monthValue, 1).lengthOfMonth()
    val safeDay = newDay.coerceIn(1, daysInMonth)
    return LocalDate.of(year, monthValue, safeDay)
}

@Composable
private fun PickerText(
    text: String,
    isSelected: Boolean,
    selectedTextColor: Color,
    unselectedTextColor: Color,
) {
    Text(
        text = text,
        style =
            (
                if (isSelected) {
                    AfternoteDesign.typography.bodyLargeR.copy(fontWeight = FontWeight.Medium)
                } else {
                    AfternoteDesign.typography.bodySmallR
                }
            ).copy(
                color = if (isSelected) selectedTextColor else unselectedTextColor,
            ),
    )
}

@Composable
private fun Divider(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "|",
        style = AfternoteDesign.typography.h3,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun DateWheelPickerPreview() {
    DateWheelPicker(
        modifier = Modifier.width(DateWheelPickerDefaults.ContainerWidth),
        currentDate = LocalDate.of(2025, 11, 26),
        onDateChange = {},
    )
}
