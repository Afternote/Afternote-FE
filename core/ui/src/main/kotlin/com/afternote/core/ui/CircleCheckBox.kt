package com.afternote.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

/**
 * 작은 직경의 원형 체크박스(기본 20.dp).
 *
 * - [onCheckedChange]가 null이면 **시각만** 그립니다. 상위에서 [Modifier.toggleable] 등으로 입력·접근성을 처리합니다.
 * - null이 아니면 [Modifier.minimumInteractiveComponentSize]와 [Role.Checkbox]·리플을 붙입니다.
 */
@Composable
fun CircleCheckBox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    checkedColor: Color = AfternoteDesign.colors.gray9,
    uncheckedColor: Color = AfternoteDesign.colors.gray4,
    checkIconColor: Color = AfternoteDesign.colors.white,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val backgroundColor by animateColorAsState(
        targetValue = if (checked) checkedColor else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "CircleCheckBoxBackground",
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) Color.Transparent else uncheckedColor,
        animationSpec = tween(durationMillis = 200),
        label = "CircleCheckBoxBorder",
    )

    Box(
        modifier =
            modifier.then(
                if (onCheckedChange != null) {
                    Modifier
                        .minimumInteractiveComponentSize()
                        .toggleable(
                            value = checked,
                            onValueChange = onCheckedChange,
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
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = backgroundColor,
            border =
                if (checked) {
                    null
                } else {
                    BorderStroke(1.5.dp, borderColor)
                },
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = checked,
                    enter = scaleIn(animationSpec = tween(150)),
                    exit = scaleOut(animationSpec = tween(150)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = checkIconColor,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CircleCheckBoxPreview() {
    AfternoteTheme {
        Row {
            CircleCheckBox(
                checked = true,
                onCheckedChange = {},
            )
            CircleCheckBox(
                checked = false,
                onCheckedChange = {},
            )
        }
    }
}
