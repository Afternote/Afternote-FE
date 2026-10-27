package com.afternote.feature.setting.presentation.notification

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.core.ui.popup.NetworkErrorPopup
import com.afternote.core.ui.popup.ServerErrorPopup
import com.afternote.feature.setting.presentation.notification.preference.NotificationPreferenceIntent
import com.afternote.feature.setting.presentation.notification.preference.NotificationPreferenceViewModel
import com.afternote.feature.setting.presentation.notification.preference.NotificationTimeSection

@Composable
internal fun PushNotificationScreen(
    onBack: () -> Unit,
    viewModel: PushNotificationViewModel = hiltViewModel(),
    timeViewModel: NotificationPreferenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeState by timeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 시각 저장 실패는 고른 값을 되돌리고 짧게 알린다. 이 화면에는 스낵바 자리가 없다.
    val timeError = timeState.errorMessage?.asString()
    ObserveSignal(
        signal = timeError,
        consumed = NotificationPreferenceIntent.ConsumeError,
        onIntent = timeViewModel::onIntent,
    ) { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onIntent(PushNotificationIntent.RefreshOnReturn) }

    PushNotificationContent(
        uiState = uiState,
        onBack = onBack,
        onRetry = { viewModel.onIntent(PushNotificationIntent.RetryLoad) },
        onNewsletterToggle = { viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.NEWSLETTER, it)) },
        onMindRecordToggle = { viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.MIND_RECORD, it)) },
        onAfternoteToggle = { viewModel.onIntent(PushNotificationIntent.TogglePushSetting(PushSetting.AFTERNOTE, it)) },
        timeSection = { NotificationTimeSection(uiState = timeState, onIntent = timeViewModel::onIntent) },
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
