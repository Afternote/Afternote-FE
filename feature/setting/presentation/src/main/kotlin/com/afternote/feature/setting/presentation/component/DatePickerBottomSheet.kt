package com.afternote.feature.setting.presentation.component

import androidx.compose.runtime.Composable
import com.afternote.core.ui.calendar.BottomSheetCalendar
import java.time.LocalDate

@Composable
fun DatePickerBottomSheet(
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate? = null,
) {
    BottomSheetCalendar(
        onDismiss = onDismiss,
        onDateSelect = onDateSelected,
        title = "전달 날짜",
        initialDate = initialDate ?: LocalDate.now(),
    )
}
