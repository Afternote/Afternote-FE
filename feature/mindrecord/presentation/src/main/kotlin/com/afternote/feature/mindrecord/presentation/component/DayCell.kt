package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray5
import com.afternote.core.ui.theme.Gray6
import com.afternote.core.ui.theme.Gray9
import com.afternote.feature.mindrecord.presentation.model.DayState
import com.afternote.feature.mindrecord.presentation.model.DayUiModel

@Composable
fun DayCell(modifier: Modifier = Modifier, day: DayUiModel) {
    if (day.day == null) {
        Box(modifier = Modifier.aspectRatio(1f))
    }

    val (bgColor, textColor) = when (day.state) {
        DayState.TODAY -> Gray9 to Color.White
        DayState.ANSWERED -> Gray2 to Gray6
        DayState.UNANSWERED -> Color.Transparent to Gray5
        DayState.NONE -> Color.Transparent to Gray5
    }
}