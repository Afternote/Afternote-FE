package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

@Composable
fun InsightCard(
    modifier: Modifier = Modifier,
    bodyText: String = stringResource(R.string.mindrecord_insight_card_default_body),
) {
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
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray6,
            )

            Spacer(modifier = Modifier.height(17.dp))

            Text(
                text = bodyText,
                style = AfternoteDesign.typography.bodySmallB,
                color = AfternoteDesign.colors.black.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(17.dp))

            Text(
                text = stringResource(R.string.mindrecord_insight_card_footer),
                style = AfternoteDesign.typography.captionLargeR,
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
