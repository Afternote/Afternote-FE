package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.afternote.core.model.MindRecordCategory
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.mindrecord.presentation.model.DayState
import com.afternote.feature.mindrecord.presentation.model.DayUiModel

@Composable
fun DayCell(
    model: DayUiModel,
    type: MindRecordCategory,
    modifier: Modifier = Modifier,
) {
    if (model.day == null) {
        Box(modifier = Modifier.aspectRatio(1f))
        return
    }

    val (bgColor, textColor) =
        when (model.state) {
            DayState.TODAY -> Color(0xFF1A1A1A) to Color.White
            DayState.ANSWERED -> Color(0xFFEEEEEE) to Color(0xFF1A1A1A)
            DayState.UNANSWERED, DayState.NONE -> Color.Transparent to AfternoteDesign.colors.gray5
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = model.day.toString(),
                    style = AfternoteDesign.typography.captionLargeB,
                    color = textColor,
                )
                if (model.state == DayState.ANSWERED || model.state == DayState.UNANSWERED) {
                    Spacer(modifier = Modifier.height(2.dp))
                    type.DayIndicator(model = model, textColor = textColor)
                }
            }
        }
    }
}
