package com.afternote.feature.afternote.presentation.detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.common.media.launchMemorialVideo
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.R
import kotlinx.coroutines.launch

/**
 * 삭제 실패 메시지를 [snackbarHostState] Snackbar 로 표출하는 [ObserveDeleteResult] 용 `onDeleteFailed` 핸들러.
 *
 * [AfternoteDetailDeleteResult.Failed] 계약대로 `messageRes` 만 표시한다.
 *
 * [ObserveDeleteResult] 는 `onDeleteFailed` 직후 `onConsumed` 로 deleteResult 를 null 로 reset 하고,
 * 그 상태 변화가 [LaunchedEffect] 를 재시작시켜 이전 effect 코루틴을 취소한다.
 * 따라서 showSnackbar 를 effect 안에서 suspend 로 직접 부르면 스낵바가 즉시 사라지므로,
 * [rememberCoroutineScope] 에 launch 해 effect 수명과 분리한다.
 *
 * `remember(snackbarHostState, resources, scope)` 의 키 3개: 반환 콜백은 이 셋을 클로저로 캡처하므로
 * 셋 다 키로 준다. 하나라도 새 인스턴스로 바뀌면 remember 가 캐시된 옛 콜백을 버리고 다시 만들어
 * 최신 값을 캡처한다 (remember 는 key 의 `equals` 변화 시 계산 람다를 재실행 —
 * https://developer.android.com/develop/ui/compose/state). 키에서 빠뜨리면 stale 캡처가 남는데,
 * 특히 `resources`(LocalResources)는 로케일·구성 변경 시 교체되므로 빠지면 옛 로케일 문자열을 조회하게 된다.
 */
@Composable
internal fun rememberDeleteFailedHandler(snackbarHostState: SnackbarHostState): (messageRes: Int) -> Unit {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    return remember(snackbarHostState, resources, scope) {
        { messageRes ->
            val message = resources.getString(messageRes)
            scope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }
}

/**
 * 상세 화면 삭제 결과 ([AfternoteDetailDeleteResult]) 공용 처리 헬퍼.
 *
 * UiState 의 nullable 신호([AfternoteDetailUiState.Success.deleteResult])를 [LaunchedEffect] 로 감지해
 * - [AfternoteDetailDeleteResult.Succeeded] → [onDeleteSucceeded] (보통 호출처가 pop 콜백을 전달)
 * - [AfternoteDetailDeleteResult.Failed] → [onDeleteFailed] (에러 UI는 화면별 Snackbar/Dialog 책임.
 *   보통 [rememberDeleteFailedHandler] 를 전달한다. 무음 삼킴 방지를 위해 필수 파라미터.)
 *
 * 처리 후 [onConsumed] 콜백으로 ViewModel 의 [AfternoteDetailIntent.ConsumeDeleteResult]
 * 호출 → state reset (재합성 시 중복 처리 방지).
 */
@Composable
internal fun ObserveDeleteResult(
    deleteResult: AfternoteDetailDeleteResult?,
    onConsumed: () -> Unit,
    onDeleteSucceeded: () -> Unit,
    onDeleteFailed: (messageRes: Int) -> Unit,
) {
    LaunchedEffect(deleteResult) {
        when (deleteResult) {
            is AfternoteDetailDeleteResult.Succeeded -> {
                onDeleteSucceeded()
                onConsumed()
            }

            is AfternoteDetailDeleteResult.Failed -> {
                onDeleteFailed(deleteResult.messageRes)
                onConsumed()
            }

            null -> {}
        }
    }
}

@Composable
internal fun AfternoteDetailNavigation(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (itemId: Long, type: AfternoteType) -> Unit,
    // **기본값을 두지 않는다.** 이 VM 은 @AssistedInject 라 인자 없는 hiltViewModel() 로 만들면
    // Hilt 가 «no creation callback was provided in CreationExtras» 로 던진다. 기본값이 있으면
    // host 가 안 넘겨도 컴파일이 통과해 상세 진입에서야 죽으므로, 넘기도록 강제한다.
    viewModel: AfternoteDetailViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val onVideoClick = rememberMemorialVideoClickHandler(snackbarHostState)

    // 수정 화면에서 저장 후 복귀하면 상세를 다시 조회한다 — 백스택에 살아 있는 동안 수정 전
    // 값이 남지 않게 한다 (#701). ON_RESUME 은 화면 off/on·홈 버튼 복귀에서도 발화하므로
    // 로딩을 방출하지 않는 refreshOnReturn() 을 쓴다. 첫 진입의 ON_RESUME 스킵(진입은 init
    // 로드가 담당)과 실행 중 로드와의 중복 차단은 VM 이 판단한다 — 결선부는 무조건 부른다.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(AfternoteDetailIntent.RefreshOnReturn)
    }

    ObserveDeleteResult(
        deleteResult = (uiState as? AfternoteDetailUiState.Success)?.deleteResult,
        onConsumed = { viewModel.onIntent(AfternoteDetailIntent.ConsumeDeleteResult) },
        onDeleteSucceeded = onNavigateBack,
        onDeleteFailed = rememberDeleteFailedHandler(snackbarHostState),
    )

    AfternoteDetailContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onNavigateToEditor = onNavigateToEditor,
        onVideoClick = onVideoClick,
    )
}

@Composable
private fun rememberMemorialVideoClickHandler(snackbarHostState: SnackbarHostState): (String) -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    return remember(context, snackbarHostState, resources, scope) {
        { videoUrl ->
            launchMemorialVideo(
                videoUrl = videoUrl,
                startActivity = context::startActivity,
                // 원인이 다르면 문구도 달라야 한다 — 둘을 한 문장으로 덮으면 스킴이 막힌
                // 상황에 «재생할 앱이 없습니다» 라는 거짓 안내가 나간다.
                onRejected = {
                    val message = resources.getString(R.string.afternote_memorial_video_invalid_url)
                    scope.launch {
                        snackbarHostState.showSnackbar(message = message)
                    }
                },
                onUnavailable = {
                    val message = resources.getString(R.string.afternote_memorial_video_no_app)
                    scope.launch {
                        snackbarHostState.showSnackbar(message = message)
                    }
                },
            )
        }
    }
}
