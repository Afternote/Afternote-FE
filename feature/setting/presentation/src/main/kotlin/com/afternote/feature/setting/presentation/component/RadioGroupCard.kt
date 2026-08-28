package com.afternote.feature.setting.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
internal fun radioGroupCardDecoration(selected: Boolean): Modifier {
    val borderColor by animateColorAsState(
        targetValue = if (selected) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray3,
        animationSpec = tween(durationMillis = 150),
        label = "RadioGroupCardBorderColor",
    )

    return Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .border(
            width = 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(6.dp),
        )
}

@Composable
internal fun RowScope.RadioGroupCardContent(
    item: RadioGroupItem,
    selected: Boolean,
) {
    Column(verticalArrangement = spacedBy(4.dp)) {
        Text(
            text = item.title,
            color =
                if (selected) {
                    AfternoteDesign.colors.gray9
                } else {
                    AfternoteDesign.colors.gray8
                },
            style =
                if (selected) {
                    AfternoteDesign.typography.primaryButton
                } else {
                    AfternoteDesign.typography.bodyBase
                },
        )
        Text(
            text = item.description,
            color = AfternoteDesign.colors.gray6,
            style = AfternoteDesign.typography.bodySmallR,
        )
    }
}
