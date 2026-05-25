package com.afternote.feature.mindrecord.presentation.component

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
 * "전체" 칩은 항상 첫 자리에 노출.
 *
 * **현재 임시 mock 단계**: 수신자측 마음의 기록 list API(`/api/v1/receiver-auth/mind-records`)
 * 응답에 카테고리 필드가 없고, receiver-auth 하위에 카테고리 마스터 엔드포인트도 없다
 * (`/api/v1/deep-thought/categories` 는 sender 전용). 따라서 현재 칩은 디자인 정합 차원의
 * 시각적 표시만 가능하며, 선택은 UI 상태로만 보관되고 record 필터링에 반영되지 않는다.
 * Notion `Mind-Record 조회` 페이지에 누락 사항 기록 — 백엔드가 list 응답에 category 를
 * 추가하거나 receiver-auth 카테고리 마스터 엔드포인트를 추가하면 실 데이터로 교체.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiverCategoryChips(
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
