package com.afternote.feature.afternote.presentation.author.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.icon.ArrowIcon
import com.afternote.core.ui.modifierextention.FadingEdgeDirection
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.modifierextention.horizontalFadingEdge
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.shared.util.TYPE_FILTER_TABS
import com.afternote.feature.afternote.presentation.shared.util.typeLabelResFor

internal const val AFTERNOTE_CATEGORY_MORE_INDICATOR_TEST_TAG = "afternoteCategoryMoreIndicator"

/**
 * 종류 필터 탭 행. `null` 은 "전체" 탭이다.
 *
 * 노출 대상은 [TYPE_FILTER_TABS] 가 정한다.
 */
@Composable
fun AfternoteTypeFilterRow(
    onTabSelected: (AfternoteType?) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: AfternoteType? = null,
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

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .bottomBorder(color = AfternoteDesign.colors.gray2, width = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    // 화살표가 쓰는 폭을 뺀 나머지가 탭의 가용 폭이다. fillMaxWidth 로 두면
                    // 화살표가 탭 위에 겹쳐 마지막 탭의 글자를 가린다.
                    .weight(1f)
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
            TYPE_FILTER_TABS.forEach { tab ->
                TypeFilterItem(
                    isSelected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    type = tab,
                )
            }
        }

        // 오른쪽 끝의 스크롤 가능 힌트. 최종 시안(4163:21839)에도 별도 reaction 이 없어 장식으로만 둔다.
        if (canScrollRight) {
            ArrowIcon(
                iconRes = R.drawable.afternote_ic_arrow_right_tab,
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(start = 8.dp)
                        .size(16.dp)
                        .testTag(AFTERNOTE_CATEGORY_MORE_INDICATOR_TEST_TAG),
            )
        }
    }
}

/**
 * 개별 탭 아이템 컴포넌트
 */
@Composable
private fun TypeFilterItem(
    type: AfternoteType?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .selectable(
                    selected = isSelected,
                    onClick = onClick,
                    role = Role.Tab,
                ),
    ) {
        Text(
            text = stringResource(typeLabelResFor(type)),
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
