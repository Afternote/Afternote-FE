package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.MindRecordCategory

@Composable
fun WeeklyReportReviewCard(modifier: Modifier = Modifier) {
    val report =
        listOf<Pair<Int, MindRecordCategory>>(
            5 to MindRecordCategory.DAILY_QUESTION,
            4 to MindRecordCategory.DIARY,
            3 to MindRecordCategory.DEEP_THOUGHT,
        )
    OutlinedCard(
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier =
            modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(6.dp)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val brush =
                            Brush.radialGradient(
                                colorStops =
                                    arrayOf(
                                        0.0f to Color(0xFFB7C4CD).copy(alpha = 0.9f),
                                        1.0f to Color(0xFFF8F8F7),
                                    ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.height * 3f,
                            )

                        onDrawBehind {
                            drawRect(brush)
                        }
                    }.padding(24.dp),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                //
                Text(
                    text = "WEEKLY SUMMARY",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFF000000).copy(0.3f),
                )

                Icon(
                    painter = painterResource(R.drawable.mindrecord_up),
                    contentDescription = null,
                )
            }

            Text(
                text = "11월 2주차 리포트",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF000000).copy(0.9f),
            )

            Text(
                text = "2025.11.10. - 2025.11.16.",
                style = MaterialTheme.typography.titleSmall,
                color = AfternoteDesign.colors.gray6,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                report.forEach { (count, category) ->
                    Column {
                        Text(
                            text = count.toString(),
                            color = Color(0xFF000000).copy(0.9f),
                        )

                        Text(
                            text = category.title,
                            color = Color(0xFF000000).copy(0.4f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyReportScreenPreview() {
    AfternoteTheme {
        WeeklyReportReviewCard()
    }
}
