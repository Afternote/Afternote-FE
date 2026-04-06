package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray6
import com.afternote.core.ui.theme.Gray9

@Composable
fun DailyQuestionWriteHeaderCard(modifier: Modifier = Modifier) {
    OutlinedCard(
        border = BorderStroke(1.dp, color = Gray2),
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
            Text(
                text = "TODAY'S QUESTION",
                style = MaterialTheme.typography.displaySmall,
                color = Gray6,
            )

            Spacer(modifier = Modifier.height(7.5.dp))
            Text(
                text = "오늘 하루, \n 누구에게 가장 고마웠나요?",
                style = MaterialTheme.typography.headlineSmall,
                color = Gray9,
            )

            Spacer(modifier = Modifier.height(7.5.dp))
            Text(
                text = "· Day 21",
                style = MaterialTheme.typography.displaySmall,
                color = Gray6,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailyQuestionWriteHeaderCardPreview() {
    AfternoteTheme {
        DailyQuestionWriteHeaderCard()
    }
}
