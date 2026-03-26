package com.afternote.afternote_fe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.datastore.TokenManager
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.feature.onboarding.presentation.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AfternoteTheme {
                val isLoggedIn by tokenManager.isLoggedInFlow.collectAsStateWithLifecycle(
                    initialValue = null,
                )

                // null 이면 DataStore 에서 아직 로그인 상태를 로딩 중 —
                // 빈 화면을 잠깐 보여줌으로써 로그인↔메인 화면 사이의 불필요한 전환(플래시)을 방지합니다.
                if (isLoggedIn == null) return@AfternoteTheme

                Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                    if (isLoggedIn == true) {
                        // TODO: 메인 화면 네비게이션으로 교체
                        MainPlaceholder()
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                // isLoggedInFlow 가 true 로 바뀌면 자동으로 MainPlaceholder 로 전환
                            },
                        )
                    }
                }
            }
        }
    }
}
