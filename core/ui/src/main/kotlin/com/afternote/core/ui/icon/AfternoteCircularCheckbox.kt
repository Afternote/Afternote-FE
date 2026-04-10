package com.afternote.core.ui.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 원형 커스텀 체크박스의 시각·상호작용 상태.
 *
 * - [Checked]: 짙은 배경([AfternoteDesign.colors.gray9]) + 흰색 체크
 * - [CheckedDisabled]: 회색 배경([AfternoteDesign.colors.gray4]) + 흰색 체크, 탭 불가
 * - [Unchecked]: 투명 + [AfternoteDesign.colors.gray4] 테두리
 */
enum class AfternoteCircularCheckboxState {
    Checked,
    CheckedDisabled,
    Unchecked,
}

/**
 * 피그마 기준 원형 체크박스.
 * 전체 지름은 [iconSize] + [contentPadding] × 2에 맞춰 자연스럽게 결정된다.
 *
 * [onClick]이 있으면 최소 48×48dp 터치 영역·리플·[Role.Checkbox] [Modifier.toggleable]을 사용합니다.
 * 부모 행이 클릭을 처리하는 경우 [onClick]을 null로 두면 시각 크기만 차지합니다.
 *
 * @param state [AfternoteCircularCheckboxState]
 * @param onClick 탭 시 콜백 ([AfternoteCircularCheckboxState.CheckedDisabled]이면 호출되지 않음)
 * @param iconSize 체크 아이콘(또는 미체크 시 공간 유지용 [Spacer]) 한 변 크기 (기본 14.dp)
 * @param contentPadding 아이콘 주변 여백 (기본 4.dp)
 */
@Composable
fun AfternoteCircularCheckbox(
    state: AfternoteCircularCheckboxState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    iconSize: Dp = 14.dp,
    contentPadding: Dp = 4.dp,
) {
    val colors = AfternoteDesign.colors
    val backgroundColor: Color =
        when (state) {
            AfternoteCircularCheckboxState.Checked -> colors.gray9
            AfternoteCircularCheckboxState.CheckedDisabled -> colors.gray4
            AfternoteCircularCheckboxState.Unchecked -> Color.Transparent
        }

    val borderWidth: Dp =
        if (state == AfternoteCircularCheckboxState.Unchecked) {
            if (iconSize < 16.dp) 1.5.dp else 2.dp
        } else {
            0.dp
        }
    val borderColor = colors.gray4

    val checked =
        state == AfternoteCircularCheckboxState.Checked ||
            state == AfternoteCircularCheckboxState.CheckedDisabled
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier.then(
                if (onClick != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .toggleable(
                            value = checked,
                            enabled = state != AfternoteCircularCheckboxState.CheckedDisabled,
                            onValueChange = { onClick() },
                            role = Role.Checkbox,
                            interactionSource = interactionSource,
                            indication = ripple(),
                        )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(color = backgroundColor, shape = CircleShape)
                    .then(
                        if (borderWidth > 0.dp) {
                            Modifier.border(
                                width = borderWidth,
                                color = borderColor,
                                shape = CircleShape,
                            )
                        } else {
                            Modifier
                        },
                    ).padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.core_ui_ic_check),
                    contentDescription = null,
                    tint = colors.white,
                    modifier = Modifier.size(iconSize),
                )
            } else {
                Spacer(modifier = Modifier.size(iconSize))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteCircularCheckboxPreview() {
    AfternoteTheme {
        var checked by remember { mutableStateOf(true) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AfternoteCircularCheckbox(
                state =
                    if (checked) {
                        AfternoteCircularCheckboxState.Checked
                    } else {
                        AfternoteCircularCheckboxState.Unchecked
                    },
                onClick = { checked = !checked },
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteCircularCheckbox(
                state = AfternoteCircularCheckboxState.CheckedDisabled,
                onClick = {},
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteCircularCheckbox(
                state = AfternoteCircularCheckboxState.Unchecked,
                onClick = { checked = true },
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteCircularCheckbox(
                state = AfternoteCircularCheckboxState.Unchecked,
                onClick = { checked = true },
                iconSize = 20.dp,
                contentPadding = 8.dp,
            )
        }
    }
}
