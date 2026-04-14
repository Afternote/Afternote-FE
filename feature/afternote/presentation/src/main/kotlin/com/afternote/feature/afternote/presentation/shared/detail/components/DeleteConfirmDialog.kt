package com.afternote.feature.afternote.presentation.shared.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.afternote.presentation.R

@Composable
fun DeleteConfirmDialog(
    serviceName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(R.string.feature_afternote_dialog_delete_title)
    val body = stringResource(R.string.feature_afternote_dialog_delete_body, serviceName)
    Popup(
        type = PopupType.Variant2,
        message = "$title\n\n$body",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.feature_afternote_dialog_delete_cancel),
        confirmText = stringResource(R.string.feature_afternote_dialog_delete_confirm),
    )
}

@Preview(showBackground = true)
@Composable
private fun DeleteConfirmDialogPreview() {
    AfternoteTheme {
        DeleteConfirmDialog(
            serviceName = "인스타그램",
            onDismiss = {},
            onConfirm = {},
        )
    }
}
