package com.afternote.feature.setting.presentation.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.popup.NetworkErrorPopup
import com.afternote.core.ui.popup.ServerErrorPopup

@Composable
internal fun PushNotificationScreen(
    onBack: () -> Unit,
    viewModel: PushNotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(PushNotificationIntent.RefreshOnReturn) }

    PushNotificationContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = { viewModel.onIntent(PushNotificationIntent.RetryLoad) },
        onNewsletterToggle = { viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, it)) },
        onMindRecordToggle = { viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.MIND_RECORD, it)) },
        onAfternoteToggle = { viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.AFTERNOTE, it)) },
    )

    when (uiState.saveFailure) {
        PushNotificationSaveFailure.NETWORK -> {
            NetworkErrorPopup(
                onRetry = { viewModel.onIntent(PushNotificationIntent.RetrySave) },
                onDismiss = { viewModel.onIntent(PushNotificationIntent.DismissSaveFailure) },
            )
        }

        PushNotificationSaveFailure.SERVER -> {
            ServerErrorPopup(
                onRetry = { viewModel.onIntent(PushNotificationIntent.RetrySave) },
                onDismiss = { viewModel.onIntent(PushNotificationIntent.DismissSaveFailure) },
            )
        }

        null -> {}
    }
}
