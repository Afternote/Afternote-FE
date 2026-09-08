package com.afternote.core.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.R
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign

enum class AfternoteButtonType {
    Default,
    Active,
    Plain,
    Un,
    Variant5,
}

/** [AfternoteButton] 공통 셰이프·라벨 세로 패딩 — 단일/dual-action 두 렌더 경로가 공유. */
private val AfternoteButtonShape = RoundedCornerShape(6.dp)
private val AfternoteButtonVerticalPadding = 13.dp

/**
 * 공통 텍스트 버튼. [type] 으로 배경/전경/보더 스타일을 고른다.
 *
 * [AfternoteButtonType.Variant5] + [secondaryText] 조합은 가운데 divider 양쪽에 두 라벨을 그린다.
 * 이때 [onSecondaryClick] 까지 주면 좌/우 절반이 **독립 클릭 타깃**인 dual-action 바가 되고
 * (예: 전체 삭제 | 선택 삭제), 생략하면 버튼 전체가 [onClick] 하나로 눌리는 기존 동작 그대로다.
 *
 * @param onSecondaryClick Variant5 dual-action 모드에서 오른쪽 절반([secondaryText]) 클릭 콜백
 * @param isSecondaryEnabled dual-action 의 **보조 라벨만** 비활성. 선택이 0개일 때 «선택 삭제» 를
 *   막는 자리다 (#442) — 자체 구현에는 있던 상태라 정본이 담지 않으면 수렴이 성립하지 않는다.
 * @param isLoading true 면 라벨 대신 스피너를 그리고 클릭을 막는다 (네트워크 대기 등 진행 중 표시).
 *   dual-action 모드도 로딩 중엔 단일 스피너 바로 렌더되어 양쪽 클릭이 모두 막힌다.
 *   접근성 이름은 [text] 로 유지되고, 로딩 상태는 stateDescription 으로 노출된다.
 */
@Composable
fun AfternoteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: AfternoteButtonType = AfternoteButtonType.Default,
    isLoading: Boolean = false,
    secondaryText: String? = null,
    containerColor: Color? = null,
    onSecondaryClick: (() -> Unit)? = null,
    isSecondaryEnabled: Boolean = true,
) {
    // 색 테이블을 여기서 한 번만 결정해 두 렌더 경로(단일 Surface·dual-action)에 같은 값을 공급한다 —
    // dual 쪽에 Variant5 색을 재하드코딩하면 테이블 변경 시 조용히 어긋난다.
    val resolvedContainerColor =
        containerColor
            ?: when (type) {
                AfternoteButtonType.Default -> AfternoteDesign.colors.gray9
                AfternoteButtonType.Active -> AfternoteDesign.colors.gray6
                AfternoteButtonType.Plain -> AfternoteDesign.colors.gray2
                AfternoteButtonType.Un -> AfternoteDesign.colors.gray2
                AfternoteButtonType.Variant5 -> AfternoteDesign.colors.gray9
            }
    // 바로 위 containerColor 와 같은 enum 이다 — else 로 닫으면 타입이 늘 때 배경색만 컴파일 에러로
    // 잡히고 글자색은 조용히 흰색으로 굳는다. 두 분기가 같은 시점에 깨지도록 항목을 모두 적는다.
    val contentColor =
        when (type) {
            AfternoteButtonType.Plain -> AfternoteDesign.colors.gray9

            AfternoteButtonType.Un -> AfternoteDesign.colors.gray5

            AfternoteButtonType.Default,
            AfternoteButtonType.Active,
            AfternoteButtonType.Variant5,
            -> AfternoteDesign.colors.white
        }
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified,
    ) {
        // 로딩 중엔 dual-action 도 이 분기를 타지 않고 아래 단일 Surface 의 스피너·클릭 차단 경로로 합류한다 —
        // dual 경로에 스피너를 따로 구현하면 isLoading 계약이 두 벌로 갈라진다.
        if (type == AfternoteButtonType.Variant5 && secondaryText != null && onSecondaryClick != null && !isLoading) {
            DualActionButtonSurface(
                text = text,
                onClick = onClick,
                secondaryText = secondaryText,
                onSecondaryClick = onSecondaryClick,
                isSecondaryEnabled = isSecondaryEnabled,
                containerColor = resolvedContainerColor,
                contentColor = contentColor,
                modifier = modifier,
            )
            return@CompositionLocalProvider
        }
        // 스피너가 라벨을 대체해도 버튼의 접근성 이름([text])과 로딩 상태는 남겨야 한다 —
        // 스크린리더가 "이름 없는 버튼" 이 되는 것을 막는 최소 semantics.
        val loadingStateDescription = stringResource(R.string.core_ui_button_loading)
        Surface(
            onClick = onClick,
            modifier =
                modifier
                    .fillMaxWidth()
                    .then(
                        if (isLoading) {
                            Modifier.semantics {
                                contentDescription = text
                                stateDescription = loadingStateDescription
                            }
                        } else {
                            Modifier
                        },
                    ),
            enabled = type != AfternoteButtonType.Un && !isLoading,
            shape = AfternoteButtonShape,
            color = resolvedContainerColor,
            contentColor = contentColor,
            border =
                if (type == AfternoteButtonType.Plain || type == AfternoteButtonType.Un) {
                    BorderStroke(
                        1.dp,
                        AfternoteDesign.colors.gray3,
                    )
                } else {
                    null
                },
        ) {
            Row(
                modifier = Modifier.padding(vertical = AfternoteButtonVerticalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                when {
                    // 로딩 중엔 라벨 대신 스피너 — 배경/높이는 유지하고 클릭은 위 Surface enabled 로 막는다.
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = contentColor,
                            strokeWidth = 2.dp,
                        )
                    }

                    type == AfternoteButtonType.Variant5 && secondaryText != null -> {
                        Text(
                            text = text,
                            style = AfternoteDesign.typography.captionLargeB,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        Variant5LabelDivider()
                        Spacer(modifier = Modifier.width(24.dp))
                        Text(
                            text = secondaryText,
                            style = AfternoteDesign.typography.captionLargeB,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    else -> {
                        Text(
                            text = text,
                            style = AfternoteDesign.typography.captionLargeB,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Variant5 dual-action 렌더링: 정적 [Surface] 안에서 좌/우 절반이 각각 clickable.
 * 단일 Surface onClick 으로는 두 액션을 구분할 수 없어 별도 분기한다.
 * 라벨은 각 절반의 중앙 정렬 — divider 에 붙는 단일 클릭 Variant5 레이아웃과 다르다.
 * 색은 [AfternoteButton] 의 타입별 색 테이블에서 받는다 (재하드코딩 금지).
 */
@Composable
private fun DualActionButtonSurface(
    text: String,
    onClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    isSecondaryEnabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AfternoteButtonShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DualActionLabel(text = text, onClick = onClick)
            Variant5LabelDivider()
            DualActionLabel(
                text = secondaryText,
                onClick = onSecondaryClick,
                isEnabled = isSecondaryEnabled,
            )
        }
    }
}

/** dual-action 좌/우 절반 하나: 절반 전체가 클릭 타깃, 라벨은 중앙 정렬. */
@Composable
private fun RowScope.DualActionLabel(
    text: String,
    onClick: () -> Unit,
    isEnabled: Boolean = true,
) {
    Box(
        modifier =
            Modifier
                .weight(1f)
                // 절반씩 독립 클릭이라 접근성 트리에도 버튼으로 잡혀야 한다. 없으면
                // 스크린리더가 이 영역을 눌 수 있는 요소로 읽지 않는다 (#634).
                .clickable(role = Role.Button, enabled = isEnabled, onClick = onClick)
                .padding(vertical = AfternoteButtonVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.captionLargeB,
            // 비활성은 색으로도 드러나야 한다 — 눌리지 않는데 눌릴 것처럼 보이면 고장으로 읽힌다.
            color = if (isEnabled) LocalContentColor.current else AfternoteDesign.colors.gray6,
        )
    }
}

/** Variant5 라벨 사이 세로 구분선 — 단일/dual-action 레이아웃이 같은 스펙(12dp·gray2)을 공유. */
@Composable
private fun Variant5LabelDivider() {
    VerticalDivider(
        modifier = Modifier.height(12.dp),
        color = AfternoteDesign.colors.gray2,
    )
}

@Composable
fun AfternoteActionButton(
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = AfternoteDesign.colors.white,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style =
                    AfternoteDesign.typography.bodySmallB.copy(fontSize = 13.sp, lineHeight = 19.5.sp),
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(9.dp))
            RightArrowIcon(modifier = Modifier.size(width = 5.dp, height = 9.dp))
        }
    }
}
