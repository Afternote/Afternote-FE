package com.afternote.feature.afternote.presentation.author.editor.processing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afternote.core.ui.form.SelectableRadioCard
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray6
import com.afternote.core.ui.theme.Gray9
import com.afternote.core.ui.theme.naNumGothic
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodOption

/**
 * Title + description content for use inside [SelectableRadioCard].
 * Figma: title 16sp Medium Gray9, description 14sp Regular Gray6, 6.dp spacing.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun OptionRadioCardContent(
    option: ProcessingMethodOption,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = option.title,
            style =
                TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontFamily = naNumGothic,
                    fontWeight = FontWeight.Medium,
                    color = Gray9,
                ),
        )
        Text(
            text = option.description,
            style =
                TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = naNumGothic,
                    fontWeight = FontWeight.Normal,
                    color = Gray6,
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
