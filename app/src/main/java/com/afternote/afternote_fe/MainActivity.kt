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
import com.afternote.afternote_fe.deeplink.AppLinkIntentContract
import com.afternote.afternote_fe.navigation.AppLinkResumeEffect
import com.afternote.afternote_fe.navigation.AppNavigation
import com.afternote.afternote_fe.navigation.rememberAfternoteAppState
import com.afternote.afternote_fe.notification.NotificationIntentContract
import com.afternote.afternote_fe.update.ForceUpdateGate
import com.afternote.afternote_fe.update.ForceUpdatePopup
import com.afternote.core.common.deeplink.AppLinkResolution
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
        // 재생성은 **같은** Intent 를 다시 배달한다. 그 링크가 어디까지 처리됐는지는 이미
        // MainViewModel 의 SavedState 에 있으므로, 다시 읽으면 소비한 목적지를 한 번 더 연다.
        if (savedInstanceState == null) enqueueAppLinkIntent(intent)

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
                    // 링크 재개 adapter 가 루트 백스택을 써야 해서 셸이 소유하던 상태를 여기로 올린다.
                    val appState = rememberAfternoteAppState()
                    AppNavigation(startDestination = route, appState = appState)

                    val resumableTarget by viewModel.resumableAppLinkTarget.collectAsStateWithLifecycle()
                    AppLinkResumeEffect(
                        appState = appState,
                        resumableTarget = resumableTarget,
                        onTargetResumed = viewModel::consumeAppLinkTarget,
                    )
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
        // warm 재진입은 사용자가 방금 누른 새 행동이라 언제나 큐에 넣는다.
        enqueueAppLinkIntent(intent)
    }

    private fun enqueueNotificationIntent(intent: Intent) {
        NotificationIntentContract
            .fromIntent(intent)
            ?.let(viewModel::enqueueNotificationEntry)
    }

    /**
     * 링크 진입을 목적지 큐에 올린다. cold start 와 warm `onNewIntent` 가 같은 계약을 지난다.
     *
     * 거절도 값이라 그대로 큐에 넣는다 — 링크가 우리 intent-filter 를 통과해 앱을 이미 열었으므로
     * 아무 데도 가지 않고 멈추는 대신 계약이 정한 안전한 기본 진입으로 보내고, 그때 기다리던
     * 옛 목적지는 이 값으로 덮여 폐기된다. 사유는 남긴다 — 서버가 앱보다 먼저 새 경로를
     * 배포했는지, 남이 도메인을 흉내 냈는지는 집계돼야 갈린다. 원본 링크는 싣지 않는다(계약 밖
     * 링크에는 ID 가 실린 경로나 query 가 그대로 들어 있을 수 있다).
     */
    private fun enqueueAppLinkIntent(intent: Intent) {
        val resolution = AppLinkIntentContract.fromIntent(intent) ?: return
        if (resolution is AppLinkResolution.Rejected) {
            errorReporter.recordFailure(
                RejectedAppLinkException(),
                mapOf(
                    "stage" to "app_link_parse",
                    "app_link_rejection" to resolution.reason.reportValue,
                ),
            )
        }
        viewModel.enqueueAppLinkTarget(resolution.target)
    }
}

/** 거절된 링크를 non-fatal 로 남길 때 쓰는 표식. 문구는 [ErrorReporter] 가 어차피 버린다. */
private class RejectedAppLinkException : IllegalArgumentException()
