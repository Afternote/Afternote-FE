package com.afternote.afternote_fe.notification

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.R
import kotlinx.coroutines.launch

/**
 * Android 13(API 33)+ 에서 `POST_NOTIFICATIONS` 런타임 권한을 1회 요청한다 (#1454).
 *
 * 이 권한이 없으면 13+ 기기에서는 일일 알림도 FCM 도 **한 건도 게시되지 않는다** — 두 생산 코드가
 * `checkSelfPermission` 으로 스스로 건너뛰고, 백그라운드 트레이 알림도 OS 가 막는다.
 * pre-33 은 플랫폼에 권한 자체가 없고 알림이 정상 게시되므로 아무것도 하지 않는다.
 *
 * 거부하면 [SnackbarHostState] 로 시스템 알림 설정행을 안내한다 — 설정 > 푸시 알림 화면이 이미
 * 같은 목적지를 갖고 있어, 새 안내 화면을 만드는 대신 그 경로로 잇는다.
 */
@Composable
fun NotificationPermissionEffect(
    snackbarHostState: SnackbarHostState,
    viewModel: NotificationPermissionViewModel = hiltViewModel(),
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shouldRequest by viewModel.shouldRequest.collectAsStateWithLifecycle()
    val deniedMessage = stringResource(R.string.notification_permission_denied_message)
    val deniedActionLabel = stringResource(R.string.notification_permission_denied_action)

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // 허용이든 거부든 물었다는 사실은 남긴다 — 매 실행마다 다시 묻지 않기 위해서다.
            viewModel.markRequested()
            if (isGranted) return@rememberLauncherForActivityResult

            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = deniedMessage,
                        actionLabel = deniedActionLabel,
                    )
                if (result == SnackbarResult.ActionPerformed) {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        },
                    )
                }
            }
        }

    LaunchedEffect(shouldRequest) {
        if (!shouldRequest) return@LaunchedEffect

        val alreadyGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            // 사용자가 시스템 설정에서 먼저 켠 경우. 물을 이유가 없으므로 기록만 남기고 끝낸다.
            viewModel.markRequested()
            return@LaunchedEffect
        }

        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
