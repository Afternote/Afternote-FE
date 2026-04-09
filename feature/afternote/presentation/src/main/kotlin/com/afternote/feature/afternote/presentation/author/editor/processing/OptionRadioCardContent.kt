package com.afternote.feature.afternote.presentation.author.editor.processing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.SelectableRadioCard
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodOption

/**
 * Title + description content for use inside [SelectableRadioCard].
 * Figma: title 16sp Medium AfternoteDesign.colors.gray9, description 14sp Regular AfternoteDesign.colors.gray6, 6.dp spacing.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun OptionRadioCardContent(
    option: ProcessingMethodOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = option.title,
            style =
                AfternoteDesign.typography.textField.copy(
                    fontWeight = FontWeight.Medium,
                    color = AfternoteDesign.colors.gray9,
                ),
        )
        Text(
            text = option.description,
            style =
                AfternoteDesign.typography.bodySmallR.copy(
                    color = AfternoteDesign.colors.gray6,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OptionRadioCardContentPreview() {
    AfternoteTheme {
        SelectableRadioCard(
            selected = true,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            content = {
                OptionRadioCardContent(
                    option = AccountProcessingMethod.MEMORIAL_ACCOUNT,
                    selected = true,
                )
            },
        )
    }
}
