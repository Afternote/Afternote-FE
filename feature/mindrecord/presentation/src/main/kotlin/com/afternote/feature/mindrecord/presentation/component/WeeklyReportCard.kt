package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun WeeklyReportCard(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier.border(
                1.dp,
                color = AfternoteDesign.colors.gray2,
                shape = RoundedCornerShape(6.dp),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(21.dp),
        ) {
            Text(
                text = "주간 리포트",
                style = AfternoteDesign.typography.bodyLargeB,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "이번 주 나의 기록들을 확인해 보세요.",
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray5,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = "42",
                        style =
                            AfternoteDesign.typography.h2.copy(
                                lineHeight = 32.sp,
                            ),
                    )

                    Text(
                        text = "총 기록",
                        style =
                            AfternoteDesign.typography.captionLargeR.copy(
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            ),
                        color = Color(0xFF000000).copy(alpha = 0.35f),
                    )
                }
                Spacer(modifier = Modifier.width(24.dp))
                VerticalDivider(modifier = Modifier.height(30.dp))
                Spacer(modifier = Modifier.width(24.dp))
                Column {
                    Text(
                        text = "7",
                        style =
                            AfternoteDesign.typography.h2.copy(
                                lineHeight = 32.sp,
                            ),
                    )

                    Text(
                        text = "이번 주",
                        style =
                            AfternoteDesign.typography.captionLargeR.copy(
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                            ),
                        color = Color(0xFF000000).copy(alpha = 0.35f),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyReportCardPreview() {
    AfternoteTheme { WeeklyReportCard() }
}
