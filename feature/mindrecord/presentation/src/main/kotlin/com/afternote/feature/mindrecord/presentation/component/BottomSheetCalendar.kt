package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.runtime.Composable
import java.time.LocalDate
import com.afternote.core.ui.calendar.BottomSheetCalendar as CoreBottomSheetCalendar

@Composable
fun BottomSheetCalendar(
    onDismiss: () -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    initialDate: LocalDate = LocalDate.now(),
) {
    CoreBottomSheetCalendar(
        onDismiss = onDismiss,
        onDateSelect = onDateSelect,
        title = "발송 날짜",
        initialDate = initialDate,
    )
}
