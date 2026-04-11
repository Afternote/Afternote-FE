package com.afternote.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme

@Composable
fun CircleCheckBox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            if (onCheckedChange != null) {
                modifier
                    .toggleable(
                        value = checked,
                        onValueChange = onCheckedChange,
                        role = Role.Checkbox,
                        interactionSource = interactionSource,
                        indication = ripple(),
                    )
            } else {
                modifier
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = backgroundColor,
            border =
                if (checked) {
                    null
                } else {
                    BorderStroke(1.dp, AfternoteDesign.colors.gray4)
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
                        tint = AfternoteDesign.colors.white,
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
        Column {
            CircleCheckBox(
                checked = true,
                onCheckedChange = {},
                backgroundColor = AfternoteDesign.colors.gray9,
                modifier = Modifier.size(20.dp),
            )
            CircleCheckBox(
                checked = true,
                onCheckedChange = {},
                backgroundColor = AfternoteDesign.colors.gray4,
                modifier = Modifier.size(20.dp),
            )
            CircleCheckBox(
                checked = false,
                onCheckedChange = {},
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
