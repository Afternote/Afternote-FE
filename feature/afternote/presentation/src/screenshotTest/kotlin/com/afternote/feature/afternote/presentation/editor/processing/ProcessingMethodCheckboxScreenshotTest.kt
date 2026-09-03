package com.afternote.feature.afternote.presentation.editor.processing

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
internal fun processingMethodCheckboxScreenshot() {
    AfternoteTheme {
        ProcessingMethodCheckbox(
            item = ProcessingMethodItem(localId = 1, text = "보관"),
            modifier = Modifier.padding(16.dp),
            onMoreClick = {},
            onDismissDropdown = {},
            onEditClick = {},
            onDeleteClick = {},
            onEditConfirmed = {},
        )
    }
}
