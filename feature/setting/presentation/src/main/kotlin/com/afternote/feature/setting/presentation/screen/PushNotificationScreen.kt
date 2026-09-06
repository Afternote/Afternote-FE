package com.afternote.feature.setting.presentation.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationViewModel

@Composable
fun PushNotificationScreen(
    onBack: () -> Unit,
    viewModel: PushNotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOnReturn()
    }

    PushNotificationContent(
        uiState = uiState,
        onBack = onBack,
        onDeviceAlarmClick = {
            val intent =
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            context.startActivity(intent)
        },
        onNewsletterToggle = viewModel::onNewsletterToggle,
        onMindRecordToggle = viewModel::onMindRecordToggle,
        onAfternoteToggle = viewModel::onAfternoteToggle,
        onSmsCheck = viewModel::onSmsChecked,
        onEmailCheck = viewModel::onEmailChecked,
        onPushCheck = viewModel::onPushChecked,
    )
}
