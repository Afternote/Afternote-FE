package com.afternote.feature.afternote.presentation.editor.processing

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun addItemTextFieldScreenshot() {
    AfternoteTheme {
        AddItemTextField(
            onItemAdded = {},
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
