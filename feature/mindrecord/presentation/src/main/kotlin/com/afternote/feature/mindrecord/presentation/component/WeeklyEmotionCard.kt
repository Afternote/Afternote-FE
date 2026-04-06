package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun WeeklyEmotionCard(modifier: Modifier = Modifier) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = AfternoteDesign.colors.gray2,
            ),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(100.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 17.dp, vertical = 9.dp),
        ) {
            Text(
                text = "주간 평균 감정",
                style = MaterialTheme.typography.displayMedium,
                color = AfternoteDesign.colors.gray9,
            )

            Text(
                text = "\uD83D\uDE0A 좋음",
                style = MaterialTheme.typography.displayMedium,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyEmotionCardPreview() {
    AfternoteTheme {
        WeeklyEmotionCard()
    }
}
