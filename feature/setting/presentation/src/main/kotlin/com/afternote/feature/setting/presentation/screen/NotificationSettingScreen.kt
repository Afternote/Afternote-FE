package com.afternote.feature.setting.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.DeviceAlarmOffSection
import com.afternote.feature.setting.presentation.component.SettingMenuItem
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationEvent
import com.afternote.feature.setting.presentation.viewmodel.PushNotificationViewModel

@Composable
fun NotificationSettingScreen(
    onBack: () -> Unit,
    onPushNotificationClick: () -> Unit,
    viewModel: PushNotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity<Activity>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val marketingConsentSaveFailedMessage = stringResource(R.string.marketing_consent_save_failed)
    val openNotificationSettings = {
        val intent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        context.startActivity(intent)
    }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.refreshDeviceAlarmStatus()
            if (
                !granted &&
                activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            ) {
                openNotificationSettings()
            }
        }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDeviceAlarmStatus()
    }

    LaunchedEffect(viewModel, lifecycleOwner, marketingConsentSaveFailedMessage) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    PushNotificationEvent.MarketingConsentSaveFailed -> {
                        snackbarHostState.showSnackbar(marketingConsentSaveFailedMessage)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.push_notification_title),
                onBackClick = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val permissionGranted =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) == PackageManager.PERMISSION_GRANTED
                            when (notificationPermissionAction(Build.VERSION.SDK_INT, permissionGranted)) {
                                NotificationPermissionAction.RequestPermission -> {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }

                                NotificationPermissionAction.OpenSettings -> {
                                    openNotificationSettings()
                                }
                            }
                        }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.device_alarm_setting),
                    style = AfternoteDesign.typography.bodyLargeR,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text =
                        if (uiState.isDeviceAlarmOn) {
                            stringResource(R.string.device_alarm_on)
                        } else {
                            stringResource(R.string.device_alarm_off)
                        },
                    style = AfternoteDesign.typography.captionLargeR,
                )
            }

            Spacer(Modifier.height(8.dp))
            if (uiState.isDeviceAlarmOn) {
                SettingMenuItem(
                    label = stringResource(R.string.push_notification_title),
                    onClick = onPushNotificationClick,
                )
            } else {
                DeviceAlarmOffSection(
                    uiState = uiState,
                    onSmsCheck = viewModel::onSmsChecked,
                    onEmailCheck = viewModel::onEmailChecked,
                    onPushCheck = viewModel::onPushChecked,
                )
            }
        }
    }
}
