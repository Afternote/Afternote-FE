package com.afternote.feature.setting.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.popup.NetworkErrorPopup
import com.afternote.core.ui.popup.ServerErrorPopup
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.DeviceAlarmOffSection
import com.afternote.feature.setting.presentation.component.PushToggleSection
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationSaveFailure
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationUiState
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationViewModel

@Composable
fun PushNotificationScreen(
    onBack: () -> Unit,
    viewModel: PushNotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PushNotificationContent(
        uiState = uiState,
        onBack = onBack,
        onNewsletterToggle = viewModel::onNewsletterToggle,
        onMindRecordToggle = viewModel::onMindRecordToggle,
        onAfternoteToggle = viewModel::onAfternoteToggle,
    )

    when (uiState.saveFailure) {
        PushNotificationSaveFailure.NETWORK -> {
            NetworkErrorPopup(
                onRetry = viewModel::onSaveFailureRetry,
                onDismiss = viewModel::onSaveFailureDismiss,
            )
        }

        PushNotificationSaveFailure.SERVER -> {
            ServerErrorPopup(
                onRetry = viewModel::onSaveFailureRetry,
                onDismiss = viewModel::onSaveFailureDismiss,
            )
        }

        null -> {}
    }
}
