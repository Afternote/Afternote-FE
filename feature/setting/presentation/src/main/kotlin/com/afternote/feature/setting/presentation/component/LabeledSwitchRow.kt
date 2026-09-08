package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign

@Composable
fun LabeledSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AfternoteDesign.typography.textField,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            thumbContent = {},
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = AfternoteDesign.colors.black,
                    uncheckedTrackColor = AfternoteDesign.colors.gray2,
                    checkedThumbColor = AfternoteDesign.colors.white,
                    uncheckedThumbColor = AfternoteDesign.colors.white,
                    checkedBorderColor = AfternoteDesign.colors.black,
                    uncheckedBorderColor = AfternoteDesign.colors.gray2,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LabeledSwitchRowPrev() {
    LabeledSwitchRow(label = "애프터노트", true, {})
}
