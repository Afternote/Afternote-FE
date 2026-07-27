package com.afternote.core.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.icon.RightArrowIcon
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

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
 */
@Composable
fun AfternoteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: AfternoteButtonType = AfternoteButtonType.Default,
    secondaryText: String? = null,
    containerColor: Color? = null,
    onSecondaryClick: (() -> Unit)? = null,
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
    val contentColor =
        when (type) {
            AfternoteButtonType.Plain -> AfternoteDesign.colors.gray9
            AfternoteButtonType.Un -> AfternoteDesign.colors.gray5
            else -> AfternoteDesign.colors.white
        }
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides androidx.compose.ui.unit.Dp.Unspecified,
    ) {
        if (type == AfternoteButtonType.Variant5 && secondaryText != null && onSecondaryClick != null) {
            DualActionButtonSurface(
                text = text,
                onClick = onClick,
                secondaryText = secondaryText,
                onSecondaryClick = onSecondaryClick,
                containerColor = resolvedContainerColor,
                contentColor = contentColor,
                modifier = modifier,
            )
            return@CompositionLocalProvider
        }
        Surface(
            onClick = onClick,
            modifier =
                modifier.fillMaxWidth(),
            enabled = type != AfternoteButtonType.Un,
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
                if (type == AfternoteButtonType.Variant5 && secondaryText != null) {
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
                } else {
                    Text(
                        text = text,
                        style = AfternoteDesign.typography.captionLargeB,
                    )
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
            DualActionLabel(text = secondaryText, onClick = onSecondaryClick)
        }
    }
}

/** dual-action 좌/우 절반 하나: 절반 전체가 클릭 타깃, 라벨은 중앙 정렬. */
@Composable
private fun RowScope.DualActionLabel(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(vertical = AfternoteButtonVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AfternoteDesign.typography.captionLargeB,
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

@Preview(showBackground = true, name = "Default")
@Composable
private fun AfternoteButtonDefaultPreview() {
    AfternoteTheme {
        Column {
            AfternoteButton(
                text = "시작하기",
                onClick = {},
                type = AfternoteButtonType.Default,
            )
            AfternoteButton(
                text = "활성",
                onClick = {},
                type = AfternoteButtonType.Active,
            )
            AfternoteButton(
                text = "일반",
                onClick = {},
                type = AfternoteButtonType.Plain,
            )
            AfternoteButton(
                text = "비활성",
                onClick = {},
                type = AfternoteButtonType.Un,
            )
            AfternoteButton(
                text = "로그인",
                onClick = {},
                type = AfternoteButtonType.Variant5,
                secondaryText = "회원가입",
            )
            AfternoteButton(
                text = "전체 삭제",
                onClick = {},
                type = AfternoteButtonType.Variant5,
                secondaryText = "선택 삭제",
                onSecondaryClick = {},
            )
        }
    }
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
                    AfternoteDesign.typography.bodySmallB.copy(fontSize = 13.sp),
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(9.dp))
            RightArrowIcon(modifier = Modifier.size(width = 5.dp, height = 9.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFCCCCCC)
@Composable
private fun AfternoteActionButtonPreview() {
    AfternoteTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AfternoteActionButton(
                text = "마음의 기록 남기기",
                containerColor = AfternoteDesign.colors.accent1,
                onClick = {},
            )
            AfternoteActionButton(
                text = "마음의 기록 남기기",
                containerColor = AfternoteDesign.colors.accent2,
                onClick = {},
            )
            AfternoteActionButton(
                text = "마음의 기록 남기기",
                containerColor = AfternoteDesign.colors.accent5,
                onClick = {},
            )
            AfternoteActionButton(
                text = "마음의 기록 남기기",
                containerColor = AfternoteDesign.colors.accent10,
                onClick = {},
            )
        }
    }
}
