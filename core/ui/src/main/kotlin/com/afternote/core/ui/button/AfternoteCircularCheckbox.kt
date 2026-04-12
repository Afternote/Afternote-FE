package com.afternote.core.ui.button

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.R
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

enum class CheckboxState {
    Default,
    Variant2,
    None,
}

@Composable
fun AfternoteCircularCheckbox(
    state: CheckboxState,
    onClick: (() -> Unit)?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    val isChecked = state == CheckboxState.Default || state == CheckboxState.Variant2
    val isClickable = state != CheckboxState.Variant2 && onClick != null

    val targetBackgroundColor =
        when (state) {
            CheckboxState.Default -> AfternoteDesign.colors.gray9
            CheckboxState.Variant2 -> AfternoteDesign.colors.gray4
            CheckboxState.None -> Color.Transparent
        }
    val targetBorderColor =
        if (state == CheckboxState.None) {
            AfternoteDesign.colors.gray4
        } else {
            Color.Transparent
        }

    val backgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(150),
        label = "checkbox_bg_color",
    )
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = tween(150),
        label = "checkbox_border_color",
    )

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(
                    width = if (state == CheckboxState.None) 2.dp else 0.dp,
                    color = borderColor,
                    shape = CircleShape,
                ).then(
                    if (isClickable) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = false, radius = size * 0.8f),
                            role = Role.Checkbox,
                            onClick = onClick,
                        )
                    } else {
                        Modifier.semantics { role = Role.Checkbox }
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = isChecked,
            enter = scaleIn(animationSpec = tween(150)),
            exit = scaleOut(animationSpec = tween(150)),
        ) {
            Icon(
                painter = painterResource(R.drawable.core_ui_ic_check),
                contentDescription = null,
                tint = AfternoteDesign.colors.white,
                modifier =
                    Modifier.size(
                        width = size * 0.5f,
                        height = size * 0.4f,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AfternoteCircularCheckboxPreview() {
    AfternoteTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AfternoteCircularCheckbox(
                state = CheckboxState.Default,
                onClick = {},
                size = 20.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteCircularCheckbox(
                state = CheckboxState.Variant2,
                onClick = null,
                size = 20.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AfternoteCircularCheckbox(
                state = CheckboxState.None,
                onClick = {},
                size = 20.dp,
            )
        }
    }
}
