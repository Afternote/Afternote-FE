package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.expand.horizontalFadingEdge
import com.afternote.core.ui.icon.ArrowIconSpec
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.AfternoteCategory

@Composable
fun AfternoteCategoryRow(
    onTabSelected: (AfternoteCategory) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: AfternoteCategory = AfternoteCategory.ALL,
) {
    val scrollState = rememberScrollState()
    val canScrollRight by remember {
        derivedStateOf { scrollState.canScrollForward }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdge(edgeWidth = 45.dp)
                    .horizontalScroll(scrollState),
        ) {
            AfternoteCategory.entries.forEach { tab ->
                CategoryItem(
                    isSelected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    category = tab,
                )
            }
        }

        // 오른쪽 끝에 화살표 아이콘 (스크롤 가능할 때만 표시)
        if (canScrollRight) {
            RightArrowIcon(
                iconSpec =
                    ArrowIconSpec(
                        iconRes = R.drawable.feature_afternote_ic_arrow_right_tab,
                        contentDescription = "더 보기",
                    ),
                backgroundColor = AfternoteDesign.colors.gray9,
                size = 16.dp,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = AfternoteDesign.colors.gray2,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * 개별 탭 아이템 컴포넌트
 */
@Composable
private fun CategoryItem(
    category: AfternoteCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .width(IntrinsicSize.Max)
                .clickable { onClick() },
    ) {
        Text(
            text = category.label,
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp,
                    color =
                        if (isSelected) {
                            AfternoteDesign.colors.gray7
                        } else {
                            AfternoteDesign.colors.gray4
                        },
                ),
            modifier =
                Modifier
                    .padding(16.dp),
        )
        if (isSelected) {
            HorizontalDivider(
                thickness = 2.dp,
                color = AfternoteDesign.colors.gray7,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("AssignedValueIsNeverRead")
private fun AfternoteCategoryRowPreview() {
    AfternoteTheme {
        var selectedTab by remember { mutableStateOf(AfternoteCategory.ALL) }
        AfternoteCategoryRow(
            onTabSelected = { selectedTab = it },
            selectedTab = selectedTab,
        )
    }
}
