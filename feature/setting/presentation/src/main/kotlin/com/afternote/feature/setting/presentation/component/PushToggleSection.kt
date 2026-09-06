package com.afternote.feature.setting.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState

@Composable
fun PushToggleSection(
    uiState: PushNotificationUiState,
    onNewsletterToggle: (Boolean) -> Unit,
    onMindRecordToggle: (Boolean) -> Unit,
    onAfternoteToggle: (Boolean) -> Unit,
) {
    LabeledSwitchRow(
        label = stringResource(R.string.timeletter),
        checked = uiState.isNewsletterOn,
        onCheckedChange = onNewsletterToggle,
        enabled = !uiState.isNewsletterUpdating,
    )
    LabeledSwitchRow(
        label = stringResource(R.string.mind_record),
        checked = uiState.isMindRecordOn,
        onCheckedChange = onMindRecordToggle,
        enabled = !uiState.isMindRecordUpdating,
    )
    LabeledSwitchRow(
        label = stringResource(R.string.afternote),
        checked = uiState.isAfternoteOn,
        onCheckedChange = onAfternoteToggle,
        enabled = !uiState.isAfternoteUpdating,
    )
}
