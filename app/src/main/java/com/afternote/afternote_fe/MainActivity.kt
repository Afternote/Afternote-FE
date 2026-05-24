package com.afternote.afternote_fe

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import dagger.hilt.android.AndroidEntryPoint

const val EXTRA_DEBUG_START_TIMELETTER = "debug_start_timeletter"

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEBUG_START_TIMELETTER, false)) {
            enableEdgeToEdge()
            setContent {
                AfternoteTheme {
                    AppNavigation(startDestination = Route.TimeLetter)
                }
            }
            return
        }

        // 시작 라우트가 null이면(아직 Auth 스트림 미확정) 시스템 스플래시를 유지한다.
        splashScreen.setKeepOnScreenCondition {
            viewModel.startRoute.value == null
        }

        enableEdgeToEdge()

        setContent {
            AfternoteTheme {
                val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()
                startRoute?.let { route ->
                    AppNavigation(startDestination = route)
                }
            }
        }
    }
}
