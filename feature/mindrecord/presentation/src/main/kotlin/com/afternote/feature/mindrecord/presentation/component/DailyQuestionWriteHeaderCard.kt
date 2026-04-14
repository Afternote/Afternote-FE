package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun DailyQuestionWriteHeaderCard(modifier: Modifier = Modifier) {
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
            Text(
                text = "TODAY'S QUESTION",
                style = AfternoteDesign.typography.mono,
                color = AfternoteDesign.colors.gray6,
            )

            Spacer(modifier = Modifier.height(7.5.dp))
            Text(
                text = "오늘 하루, \n누구에게 가장 고마웠나요?",
                style = AfternoteDesign.typography.h3,
                color = AfternoteDesign.colors.gray9,
            )

            Spacer(modifier = Modifier.height(7.5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "답변하러가기",
                    style = AfternoteDesign.typography.captionLargeR,
                    color = AfternoteDesign.colors.gray6,
                )
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        painter = painterResource(R.drawable.core_ui_right),
                        contentDescription = null,
                        tint = AfternoteDesign.colors.gray6,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
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
