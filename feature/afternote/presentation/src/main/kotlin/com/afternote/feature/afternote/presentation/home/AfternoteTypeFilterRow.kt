package com.afternote.feature.afternote.presentation.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
 * 탭을 감싸는 행에 [Modifier.selectableGroup] 을 건다. 각 탭이 [Role.Tab] 으로 개별 선택 상태를
 * 노출하는 것만으로는 «이들이 하나의 상호배타 그룹» 이라는 관계가 접근성 트리에 실리지 않아,
 * 스크린리더가 「N 개 중 M 번째」를 읽어 주지 못한다 (#1636). core:ui 의 `AfternoteRadioGroup` 과
 * 같은 관용구다.
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
                    .selectableGroup()
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
                            // 시안(4327:43064 의 `tab btn` 컴포넌트)의 비선택 탭은 #A0A0A0 이고,
                            // gray5(#9E9E9E)와 채널 차가 2/255 라 육안으로 구분되지 않는다.
                            // 종전 gray4(#BDBDBD)는 시안보다 밝아 대비가 1.80:1 까지 떨어져 있었다 (#1636).
                            // gray5 로도 WCAG AA 4.5:1 은 못 넘는다(2.57:1) — 그 축은 팔레트에
                            // 중간값이 없어 디자이너 결정이 필요하고 #1636 이 계속 추적한다.
                            AfternoteDesign.colors.gray5
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
