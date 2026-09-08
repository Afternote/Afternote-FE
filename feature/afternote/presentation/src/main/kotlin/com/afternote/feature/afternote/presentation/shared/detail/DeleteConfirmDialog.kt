package com.afternote.feature.afternote.presentation.shared.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.popup.Popup
import com.afternote.core.ui.popup.PopupType
import com.afternote.feature.afternote.presentation.R

@Composable
fun DeleteConfirmDialog(
    serviceName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(R.string.afternote_dialog_delete_title)
    val body = stringResource(R.string.afternote_dialog_delete_body, serviceName)
    Popup(
        type = PopupType.Variant2,
        message = "$title\n\n$body",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissText = stringResource(R.string.afternote_dialog_delete_cancel),
        confirmText = stringResource(R.string.afternote_dialog_delete_confirm),
    )
}
