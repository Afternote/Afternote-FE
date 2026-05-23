package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.feature.mindrecord.presentation.R
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
        title = stringResource(R.string.mindrecord_calendar_send_date_title),
        initialDate = initialDate,
    )
}
