package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray5
import com.afternote.core.ui.theme.Gray9
import com.afternote.feature.mindrecord.presentation.R

@Composable
fun TodayQuestionCard(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .height(230.dp)
                .drawWithCache {
                    val radius = size.height + 35.dp.toPx() // 하단 중앙 → 박스 상단 위 40px

                    val brush =
                        Brush.radialGradient(
                            colorStops =
                                arrayOf(
                                    0.0f to Color(0xFFB7CDC0),
                                    1.0f to Color(0xFFF8F8F7),
                                ),
                            center = Offset(x = size.width / 2f, y = size.height), // 하단 정중앙
                            radius = radius,
                        )

                    onDrawBehind {
                        drawRect(brush)
                    }
                }.dropShadow(
                    shape = RoundedCornerShape(0.dp),
                    shadow =
                        Shadow(
                            radius = 5.dp,
                            spread = 0.dp,
                            color = Color(0xFF000000).copy(alpha = 0.05f),
                            offset = DpOffset(x = 0.dp, 2.dp),
                        ),
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(30.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Today'S QUESTION",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFF000000).copy(alpha = 0.3f),
                )

                Icon(
                    painterResource(R.drawable.outline_wb_sunny_24),
                    contentDescription = null,
                    tint = Color(0xFF000000).copy(alpha = 0.15f),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "오늘 하루,\n 누구에게 가장 고마웠나요?",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "매일 다른 질문들에 나를 남겨보세요.",
                style = MaterialTheme.typography.displayMedium,
                color = Gray5,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = {},
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Gray9,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "마음의 기록 남기기",
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TodayQuestionCardPreview() {
    AfternoteTheme {
        TodayQuestionCard()
    }
}
