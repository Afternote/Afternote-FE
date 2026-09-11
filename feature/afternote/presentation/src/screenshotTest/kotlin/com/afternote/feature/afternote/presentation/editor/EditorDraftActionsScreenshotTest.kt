package com.afternote.feature.afternote.presentation.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 100)
@Composable
internal fun editorDraftActionsEnabledScreenshot() {
    DraftActionsPreview(isSubmitEnabled = true)
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, heightDp = 100)
@Composable
internal fun editorDraftActionsDisabledScreenshot() {
    DraftActionsPreview(isSubmitEnabled = false)
}

@Composable
private fun DraftActionsPreview(isSubmitEnabled: Boolean) {
    AfternoteTheme {
        AfternoteEditorScreen(
            form = EditorFormState(),
            onBackClick = {},
            onRegisterClick = {},
            onSaveDraftClick = {},
            snackbarMessage = null,
            onSnackbarMessageConsumed = {},
            validationMessage = null,
            onValidationMessageConsumed = {},
            content = {},
            isSubmitEnabled = isSubmitEnabled,
        )
    }
}
