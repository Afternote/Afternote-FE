package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.mindrecord.presentation.R

/**
 * 일기 카드 형 상단 요약 카드.
 *
 * Figma 2671:16734 — 흰 배경 / gray2 1dp 보더 / radius 6.
 * 좌측 "이번 달" + 카운트, 우측 "주간 평균 기분" + 이모지.
 */
@Composable
fun DiaryReportCard(
    monthDiaryCount: Int,
    weeklyMoodEmoji: String?,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        colors =
            CardDefaults.cardColors(
                containerColor = AfternoteDesign.colors.white,
            ),
        border = BorderStroke(1.dp, color = AfternoteDesign.colors.gray2),
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(17.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.mindrecord_weekly_report_this_month),
                    style = AfternoteDesign.typography.footnoteCaption,
                    color = AfternoteDesign.colors.gray6,
                )
                Text(
                    text = monthDiaryCount.toString(),
                    style = AfternoteDesign.typography.h2,
                    color = AfternoteDesign.colors.gray9,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(R.string.mindrecord_weekly_report_avg_mood_weekly),
                    style = AfternoteDesign.typography.footnoteCaption,
                    color = AfternoteDesign.colors.gray6,
                )
                Text(
                    text = weeklyMoodEmoji.orEmpty(),
                    fontSize = 24.sp,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DiaryCardPreview() {
    AfternoteTheme {
        DiaryReportCard(monthDiaryCount = 18, weeklyMoodEmoji = "😊")
    }
}
