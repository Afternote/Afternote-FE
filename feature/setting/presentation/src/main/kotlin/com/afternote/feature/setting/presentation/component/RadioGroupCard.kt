package com.afternote.feature.setting.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.CustomRadioButton
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun RadioGroupCard(
    item: RadioGroupItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) AfternoteDesign.colors.gray9 else AfternoteDesign.colors.gray3,
        animationSpec = tween(durationMillis = 150),
        label = "RadioGroupCardBorderColor",
    )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp),
                ).selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton,
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() },
                ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CustomRadioButton(
            selected = selected,
            // onClick null → Row에서 처리
        )
        Spacer(modifier = Modifier.padding(start = 16.dp))
        Column(verticalArrangement = spacedBy(4.dp)) {
            Text(
                text = item.title,
                color = AfternoteDesign.colors.gray9,
                style = AfternoteDesign.typography.primaryButton,
            )
            Text(
                text = item.description,
                color = AfternoteDesign.colors.gray6,
                style = AfternoteDesign.typography.bodySmallR,
            )
        }
    }
}
