package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.mindrecord.domain.model.MindRecordSummary

/**
 * 일기 탭의 2-column masonry 카드 (디자인 노드 1727-19688).
 *
 * 디자인은 카드별 높이가 240/216/106 등으로 가변(사진 포함 여부 + 본문 줄수에 따라).
 * 요약 데이터만으로는 사진/본문이 없으므로 1차는 *제목 + 날짜* 만 노출하는 고정 높이 형태로 처리.
 * detail API 연동 후 사진/이모지/본문 줄수에 따른 가변 높이로 확장 예정.
 */
@Composable
fun ReceiverDiaryGridCard(
    record: MindRecordSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = AfternoteDesign.colors.white,
            ),
        border = BorderStroke(1.dp, AfternoteDesign.colors.gray2),
    ) {
        Column {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .background(AfternoteDesign.colors.gray1),
                contentAlignment = Alignment.Center,
            ) {
                // detail API 연동 전 placeholder 영역. 추후 사진/이모지 노출.
                Text(
                    text = "📓",
                    style = AfternoteDesign.typography.h3,
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "${record.recordDate} · ${record.senderName}",
                    style = AfternoteDesign.typography.footnoteCaption,
                    color = AfternoteDesign.colors.gray6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = record.title,
                    style = AfternoteDesign.typography.bodySmallB,
                    color = AfternoteDesign.colors.gray9,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
