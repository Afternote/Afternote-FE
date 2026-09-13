package com.afternote.afternote_fe

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.notification.NotificationIntentContract
import com.afternote.afternote_fe.update.ForceUpdateGate
import com.afternote.afternote_fe.update.ForceUpdatePopup
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.ui.Route
import com.afternote.core.ui.theme.AfternoteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val EXTRA_DEBUG_START_TIMELETTER = "debug_start_timeletter"

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    /**
     * 강제 업데이트 판정 (#1539). 관문은 앱 프로세스 수명 단위라 ViewModel 이 아니라 싱글톤이고,
     * 화면은 그 값을 읽기만 한다 — 이 Activity 가 부르는 조회는 없다.
     */
    @Inject
    lateinit var forceUpdateGate: ForceUpdateGate

    @Inject
    lateinit var errorReporter: ErrorReporter

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableLightEdgeToEdge()

        enqueueNotificationIntent(intent)

        if (BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEBUG_START_TIMELETTER, false)) {
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

        setContent {
            AfternoteTheme {
                val startRoute by viewModel.startRoute.collectAsStateWithLifecycle()
                startRoute?.let { route ->
                    AppNavigation(startDestination = route)
                }
                val forceUpdatePrompt by forceUpdateGate.prompt.collectAsStateWithLifecycle()
                forceUpdatePrompt?.let { prompt ->
                    ForceUpdatePopup(onConfirm = { openStore(prompt.storeUrl) })
                }
            }
        }
    }

    /**
     * 스토어를 연다. 열지 못해도 앱을 죽이지 않는다 — 관문은 이미 떠 있고, 사용자는 다시 누를 수 있다.
     *
     * 여는 데 실패했다는 사실 자체는 남긴다. 여기까지 왔다는 건 서버가 준 주소가 Play Store
     * 형식은 맞았다는 뜻이라, 실패하면 기기에 그 주소를 열 수 있는 앱이 없다는 신호다.
     */
    private fun openStore(storeUrl: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, storeUrl.toUri()))
        }.onFailure { error ->
            errorReporter.recordFailure(error, mapOf("stage" to "force_update_open_store"))
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
            ?.let(viewModel::enqueueNotificationEntry)
    }
}
