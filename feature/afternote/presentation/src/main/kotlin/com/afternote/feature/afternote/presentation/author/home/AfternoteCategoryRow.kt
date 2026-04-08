package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.FadingEdgeDirection
import com.afternote.core.ui.bottomBorder
import com.afternote.core.ui.horizontalFadingEdge
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
    val needsHorizontalFade by remember {
        derivedStateOf { scrollState.maxValue > 0 }
    }
    val fadingDirection by remember {
        derivedStateOf {
            when {
                scrollState.canScrollBackward && scrollState.canScrollForward -> {
                    FadingEdgeDirection.BOTH
                }

                scrollState.canScrollBackward -> {
                    FadingEdgeDirection.LEFT
                }

                scrollState.canScrollForward -> {
                    FadingEdgeDirection.RIGHT
                }

                else -> {
                    FadingEdgeDirection.RIGHT
                }
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .bottomBorder(color = AfternoteDesign.colors.gray2, width = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (needsHorizontalFade) {
                            Modifier.horizontalFadingEdge(
                                edgeWidth = 45.dp,
                                direction = fadingDirection,
                            )
                        } else {
                            Modifier
                        },
                    ).horizontalScroll(scrollState),
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
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .pointerInput(Unit) { detectTapGestures { } },
            )
        }
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
                .selectable(
                    selected = isSelected,
                    onClick = onClick,
                    role = Role.Tab,
                ),
    ) {
        Text(
            text = category.label,
            style =
                AfternoteDesign.typography.bodySmallB.copy(
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
                        .padding(horizontal = 16.dp)
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
