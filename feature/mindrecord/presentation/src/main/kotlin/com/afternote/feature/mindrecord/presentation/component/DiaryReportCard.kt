package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray1
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray6
import com.afternote.core.ui.theme.Gray9

@Composable
fun DiaryReportCard(modifier: Modifier = Modifier) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = Gray1,
            ),
        border = BorderStroke(1.dp, color = Gray2),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "이번 달",
                    style = MaterialTheme.typography.displaySmall,
                    color = Gray6,
                )

                Text(
                    text = "주간 평균 기분",
                    style = MaterialTheme.typography.displaySmall,
                    color = Gray6,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "18",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Gray9,
                )

                Text(
                    text = "\uD83D\uDE0A",
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryCardPreview() {
    AfternoteTheme {
        DiaryReportCard()
    }
}
