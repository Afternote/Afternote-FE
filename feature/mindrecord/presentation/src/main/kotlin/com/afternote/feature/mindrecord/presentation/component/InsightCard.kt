package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun InsightCard(modifier: Modifier = Modifier) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = Color(0xFFFFFFFF),
            ),
        border = BorderStroke(1.dp, color = Color(0xFF000000).copy(alpha = 0.05f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "INSIGHTS",
                style = MaterialTheme.typography.displaySmall,
                color = AfternoteDesign.colors.gray6,
            )

            Spacer(modifier = Modifier.height(17.dp))

            Text(
                text = "이번주는 가족과 함께하는 시간을 가장 많이 기록하셨네요. 일상의 소중함을 느끼는 한주였던 것 같아요.",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF000000).copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(17.dp))

            Text(
                text = "매일 꾸준히 기록하는 습관이 정말 멋져요!",
                style = MaterialTheme.typography.displayMedium,
                color = AfternoteDesign.colors.gray6,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InsightCardPreview() {
    AfternoteTheme {
        InsightCard()
    }
}
