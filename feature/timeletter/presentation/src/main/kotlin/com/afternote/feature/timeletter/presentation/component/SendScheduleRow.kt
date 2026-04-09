package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun SendScheduleRow(
    date: String,
    time: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "발송 날짜",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier.clickable(onClick = onDateClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = date,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "날짜 변경",
                tint = AfternoteDesign.colors.gray6,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = "발송 시간",
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray6,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            modifier = Modifier.clickable(onClick = onTimeClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = time,
                style = AfternoteDesign.typography.bodySmallR,
                color = AfternoteDesign.colors.gray9,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "시간 변경",
                tint = AfternoteDesign.colors.gray6,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
