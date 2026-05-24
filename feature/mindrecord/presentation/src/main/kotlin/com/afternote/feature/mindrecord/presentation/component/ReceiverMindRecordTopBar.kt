package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.mindrecord.presentation.viewmodel.ReceiverMindRecordFilter

/**
 * 수신자 마음의 기록 상단 헤더.
 *
 * 기본 상태는 "마음의 기록" 타이틀 + 검색 아이콘. 필터가 적용된 상태(`filter.isApplied`)면
 * 타이틀 자리에 "yyyy.MM.dd - yyyy.MM.dd · 최신순" 형태의 인디케이터를 노출한다
 * (디자인 노드 1727-24804).
 */
@Composable
fun ReceiverMindRecordTopBar(
    filter: ReceiverMindRecordFilter,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (filter.isApplied) {
            AppliedFilterIndicator(filter = filter, onClick = onFilterClick)
        } else {
            Text(
                text = "마음의 기록",
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFilterClick),
        )
    }
}

@Composable
private fun AppliedFilterIndicator(
    filter: ReceiverMindRecordFilter,
    onClick: () -> Unit,
) {
    val range =
        buildString {
            append(filter.fromDate ?: "")
            if (filter.fromDate != null && filter.toDate != null) append(" - ")
            append(filter.toDate ?: "")
        }
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(AfternoteDesign.colors.gray1)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = range,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray9,
        )
        Spacer(modifier = Modifier.size(6.dp))
        Box(
            modifier =
                Modifier
                    .size(2.dp)
                    .clip(CircleShape)
                    .background(AfternoteDesign.colors.gray6),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = filter.sortOrder.label,
            style = AfternoteDesign.typography.captionLargeR,
            color = AfternoteDesign.colors.gray9,
        )
    }
}
