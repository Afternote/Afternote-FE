package com.afternote.core.ui.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
 * 하나의 [selectedValue]만 받는 공용 라디오 그룹입니다.
 *
 * 그룹에는 [Modifier.selectableGroup]을, 각 옵션 행에는 하나의 [Role.RadioButton] semantics를
 * 부여합니다. 원형 인디케이터는 입력·선택 semantics가 없는 내부 시각 요소이므로 클릭 영역과
 * 선택 상태가 중복 노출되지 않습니다. 같은 값이 둘 이상 있으면 한 값으로 여러 행이 선택될 수
 * 있으므로 중복 [options]는 허용하지 않습니다.
 *
 * [itemDecoration]은 행의 레이아웃·시각 장식만 추가하는 용도입니다. 선택 입력은 그룹이 소유하도록
 * clickable/selectable semantics를 추가하지 마세요.
 */
@Composable
fun <T : Any> AfternoteRadioGroup(
    options: List<T>,
    selectedValue: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    itemContentPadding: PaddingValues = PaddingValues(0.dp),
    itemDecoration: @Composable (T, Boolean) -> Modifier = { _, _ -> Modifier },
    itemContent: @Composable RowScope.(T, Boolean) -> Unit,
) {
    require(options.distinct().size == options.size) {
        "AfternoteRadioGroup options must be unique."
    }

    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = verticalArrangement,
    ) {
        options.forEach { option ->
            key(option) {
                val selected = option == selectedValue
                val interactionSource = remember { MutableInteractionSource() }

                Row(
                    modifier =
                        itemDecoration(option, selected)
                            .selectable(
                                selected = selected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton,
                                interactionSource = interactionSource,
                                indication = ripple(),
                            ).minimumInteractiveComponentSize()
                            .padding(itemContentPadding),
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioIndicator(selected = selected)
                    itemContent(option, selected)
                }
            }
        }
    }
}

/** 입력 semantics 없이 선택 상태만 그리는 그룹 내부 인디케이터. */
@Composable
private fun RadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
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
        label = "RadioIndicatorBorderColor",
    )
    val indicatorSize by animateDpAsState(
        targetValue = if (selected) maxIndicatorSize else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "RadioIndicatorSize",
    )

    Box(
        modifier = modifier,
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
private fun AfternoteRadioGroupPreview() {
    AfternoteTheme {
        AfternoteRadioGroup(
            options = listOf("첫 번째", "두 번째"),
            selectedValue = "첫 번째",
            onSelect = {},
            modifier = Modifier.padding(16.dp),
        ) { _, _ -> }
    }
}
