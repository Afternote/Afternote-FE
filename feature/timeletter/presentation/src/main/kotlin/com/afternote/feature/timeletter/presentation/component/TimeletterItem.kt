package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afternote.feature.timeletter.domain.TimeLetter

@Composable
fun TimeletterItem(
    letter: TimeLetter,
    modifier: Modifier = Modifier,
) {
    val identity = letter.identity
    val schedule = letter.schedule
    val typography = MaterialTheme.typography
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 22.5.dp, vertical = 12.dp),
    ) {
        Text(
            text = identity.title,
            style = typography.bodyLarge,
        )
        Text(
            text = schedule.recipientName,
            style = typography.bodySmall,
        )
        Text(
            text = schedule.openDate.value,
            style = typography.bodySmall,
        )
    }
    HorizontalDivider()
}
