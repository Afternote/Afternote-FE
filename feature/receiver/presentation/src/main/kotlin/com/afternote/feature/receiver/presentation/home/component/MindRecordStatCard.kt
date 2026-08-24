package com.afternote.feature.receiver.presentation.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.R as CoreUiR

/**
 * 마음의 기록 섹션 안에 들어가는 작은 통계 카드: 아이콘 → 라벨 → TOTAL → 카운트 (수직 정렬).
 */
@Composable
fun MindRecordStatCard(
    iconResId: Int,
    label: String,
    totalLabel: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = AfternoteDesign.colors.gray2,
                    shape = RoundedCornerShape(8.dp),
                ).background(AfternoteDesign.colors.white)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AfternoteDesign.colors.gray2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AfternoteDesign.colors.gray6,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = AfternoteDesign.typography.bodySmallB,
            color = AfternoteDesign.colors.gray9,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = totalLabel,
            style = AfternoteDesign.typography.mono,
            color = AfternoteDesign.colors.gray5,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = count.toString(),
            style = AfternoteDesign.typography.h3,
            color = AfternoteDesign.colors.gray9,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MindRecordStatCardPreview() {
    AfternoteTheme {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MindRecordStatCard(
                iconResId = CoreUiR.drawable.core_ui_ic_mindrecord,
                label = "데일리 질문",
                totalLabel = "TOTAL",
                count = 18,
                modifier = Modifier.weight(1f),
            )
            MindRecordStatCard(
                iconResId = CoreUiR.drawable.core_ui_ic_diary,
                label = "일기",
                totalLabel = "TOTAL",
                count = 18,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
