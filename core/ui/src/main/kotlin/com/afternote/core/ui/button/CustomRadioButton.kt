package com.afternote.core.ui.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * 커스텀 라디오 버튼 컴포넌트
 *
 * 체크 표시(인디케이터)와 윤곽선 간 간격이 전체 크기의 12분의 1이 되도록 자동 계산됩니다.
 * [onClick]이 있으면 바깥에 [Modifier.minimumInteractiveComponentSize]와
 * [Modifier.selectable] + [Role.RadioButton]을 두어 최소 48×48dp 터치 영역과 리플을 쓰고,
 * 안쪽은 [buttonSize]만큼만 그립니다. 부모가 선택을 처리할 때는 [onClick]을 null로 두면 24dp만 차지합니다.
 *
 * @param selected 선택 여부
 * @param onClick 클릭 이벤트 (null이면 비인터랙티브·부모에서 처리)
 * @param buttonSize 전체 버튼 크기 (기본값: 24.dp)
 * @param selectedColor 선택된 색상 (기본값: AfternoteDesign.colors.gray9)
 * @param unselectedColor 선택 안 된 색상 (기본값: AfternoteDesign.colors.gray4)
 */
@Composable
fun CustomRadioButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    buttonSize: Dp = 24.dp,
    selectedColor: Color = AfternoteDesign.colors.gray9,
    unselectedColor: Color = AfternoteDesign.colors.gray4,
) {
    val borderWidth = 1.dp
    val spacing = buttonSize / 12f
    val maxIndicatorSize = buttonSize - (borderWidth * 2) - (spacing * 2)

    val targetBorderColor = if (selected) selectedColor else unselectedColor
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(durationMillis = 150),
        label = "CustomRadioButtonBorderColor",
    )

    val indicatorSize by animateDpAsState(
        targetValue = if (selected) maxIndicatorSize else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "CustomRadioButtonIndicatorSize",
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier.then(
                if (onClick != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .selectable(
                            selected = selected,
                            onClick = onClick,
                            role = Role.RadioButton,
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
                    .size(buttonSize)
                    .clip(CircleShape)
                    .border(
                        width = borderWidth,
                        color = animatedBorderColor,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (indicatorSize > 0.dp) {
                Box(
                    modifier =
                        Modifier
                            .size(indicatorSize)
                            .background(
                                color = animatedBorderColor,
                                shape = CircleShape,
                            ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomRadioButtonPreview() {
    AfternoteTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CustomRadioButton(
                selected = true,
                onClick = {},
            )
            CustomRadioButton(
                selected = false,
                onClick = {},
            )
        }
    }
}
