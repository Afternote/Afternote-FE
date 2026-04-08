package com.afternote.core.ui.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
 *
 * [onClick]이 있으면 최소 48×48dp 터치 영역·리플·[Role.Checkbox] [Modifier.toggleable]을 사용합니다.
 * 부모 행이 [toggleable]로 처리하는 경우 [onClick]을 null로 두면 시각 크기만 차지합니다.
 *
 * @param state [AfternoteCircularCheckboxState]
 * @param onClick 탭 시 콜백 ([AfternoteCircularCheckboxState.CheckedDisabled]이면 호출되지 않음)
 * @param visualSize 원의 직경 (기본 24.dp)
 */
@Composable
fun AfternoteCircularCheckbox(
    state: AfternoteCircularCheckboxState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    visualSize: Dp = 24.dp,
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
            if (visualSize < 20.dp) 1.5.dp else 2.dp
        } else {
            0.dp
        }
    val borderColor = colors.gray4

    val checked =
        state == AfternoteCircularCheckboxState.Checked ||
            state == AfternoteCircularCheckboxState.CheckedDisabled
    val interactionSource = remember { MutableInteractionSource() }

    val checkIconSize = (visualSize * 0.55f).coerceIn(10.dp, 18.dp)

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
                    .size(visualSize)
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
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.white,
                    modifier = Modifier.size(checkIconSize),
                )
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
        }
    }
}
