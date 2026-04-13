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
import com.afternote.core.ui.theme.AfternoteTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 로딩 중이거나 시작 라우트가 아직 없으면 시스템 스플래시를 유지한다.
        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value || viewModel.startRoute.value == null
        }

        enableEdgeToEdge()

        setContent {
            AfternoteTheme {
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()

                if (!isLoading) {
                    startRoute?.let { route ->
                        AppNavigation(startDestination = route)
                    }
                }
            }
        }
    }
}
