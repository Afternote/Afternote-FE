package com.afternote.afternote_fe

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.rememberAfternoteAppState
import com.afternote.afternote_fe.notification.NotificationIntentContract
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first

const val EXTRA_DEBUG_START_TIMELETTER = "debug_start_timeletter"

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val debugStartTimeLetter =
            BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEBUG_START_TIMELETTER, false)
        enqueueNotificationIntent(intent)

        // 시작 라우트가 null이면(아직 Auth 스트림 미확정) 시스템 스플래시를 유지한다.
        splashScreen.setKeepOnScreenCondition {
            !debugStartTimeLetter && viewModel.startRoute.value == null
        }

        setContent {
            AfternoteTheme {
                val authenticatedStartRoute by viewModel.startRoute.collectAsStateWithLifecycle()
                val pendingRequest by viewModel.pendingNotificationRequest.collectAsStateWithLifecycle()
                val appState = rememberAfternoteAppState()
                val startDestination =
                    if (debugStartTimeLetter) Route.TimeLetter else authenticatedStartRoute

                startDestination?.let { route ->
                    AppNavigation(
                        startDestination = route,
                        appState = appState,
                    )

                    LaunchedEffect(authenticatedStartRoute, pendingRequest) {
                        val request = pendingRequest ?: return@LaunchedEffect
                        when (authenticatedStartRoute) {
                            Route.Home -> {
                                // NavHost가 첫 entry를 만든 뒤에만 warm/cold 알림 목적지를 push한다.
                                appState.navController.currentBackStackEntryFlow.first()
                                appState.navigateFromNotification(request.destination)
                                viewModel.consumeNotificationRequest(request.occurrenceToken)
                            }

                            Route.Onboarding -> {
                                // 알림은 인증을 우회하지 않는다. 로그아웃 상태에서는 요청만 소비한다.
                                viewModel.consumeNotificationRequest(request.occurrenceToken)
                            }

                            else -> {
                                return@LaunchedEffect
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enqueueNotificationIntent(intent)
    }

    private fun enqueueNotificationIntent(intent: Intent) {
        NotificationIntentContract
            .fromIntent(intent)
            ?.let(viewModel::enqueueNotificationRequest)
    }
}
