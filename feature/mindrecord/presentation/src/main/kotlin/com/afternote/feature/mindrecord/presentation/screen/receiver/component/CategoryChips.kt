package com.afternote.feature.mindrecord.presentation.screen.receiver.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

/**
 * 깊은생각 탭의 카테고리 칩 그룹 (디자인 노드 1727-19627 의 9개 tag).
 *
 * "전체" 칩은 항상 첫 자리에 노출. 백엔드 카테고리 마스터 미확정이라 1차는
 * 깊은생각 list 응답에서 도출한 카테고리 string 그대로 사용.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CategoryChip(
            label = "전체",
            isSelected = selected == null,
            onClick = { onSelect(null) },
        )
        categories.forEach { category ->
            CategoryChip(
                label = category,
                isSelected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.white
    val fg = if (isSelected) AfternoteDesign.colors.white else AfternoteDesign.colors.gray9
    Text(
        text = label,
        style = AfternoteDesign.typography.captionLargeR,
        color = fg,
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .border(width = 1.dp, color = AfternoteDesign.colors.gray2, shape = RoundedCornerShape(50))
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}
