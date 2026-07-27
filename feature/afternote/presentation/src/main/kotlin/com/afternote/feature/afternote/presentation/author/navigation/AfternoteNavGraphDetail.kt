package com.afternote.feature.afternote.presentation.author.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavBackStackEntry
import androidx.navigation.toRoute
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailDeleteResult
import com.afternote.feature.afternote.presentation.author.detail.GalleryDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.MemorialDetailRoute
import com.afternote.feature.afternote.presentation.author.detail.account.AccountDetailRoute
import com.afternote.feature.afternote.presentation.author.navigation.model.AfternoteRoute
import kotlinx.coroutines.launch

@Composable
internal fun DetailLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun DesignPendingDetailContent(onBackClick: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(title = "", onBackClick = onBackClick)
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.design_pending))
        }
    }
}

/**
 * 상세 데이터 로드 실패 화면.
 *
 * [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState.Error] 계약대로
 * [rawMessage](서버 메시지 등)를 우선 사용하고, 비어 있으면 [messageRes] 를 [stringResource] 로 변환한다
 * (둘 다 없으면 [R.string.afternote_detail_load_error] 폴백).
 *
 * 표시 방식 통일(#446) 결론이 나오면 이 컴포저블의 본문 표현만 교체한다 — Route 의 Error 분기 배선은 유지.
 * [DesignPendingDetailContent] 는 진짜 미디자인 폴백(blank itemId·contentUiModel 타입 불일치)용으로 별도 유지.
 *
 * @param rawMessage 서버·예외가 런타임에 준 이미 완성된 표시 문자열. 있으면 그대로 쓴다(동적 외부 문구).
 * @param messageRes 앱에 박힌 문자열 리소스 ID(`R.string.*`). `@StringRes` 는 이 Int 가 임의 정수가 아니라
 *   string 리소스 id 임을 Lint 에 알리는 표식이며, [stringResource] 로 실제 텍스트로 변환한다
 *   (정적 내장 문구 — rawMessage 가 비었을 때의 로컬라이즈 가능한 폴백).
 */
@Composable
internal fun DetailLoadErrorContent(
    rawMessage: String?,
    @StringRes messageRes: Int?,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            DetailTopBar(title = "", onBackClick = onBackClick)
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    rawMessage?.takeIf { it.isNotBlank() }
                        ?: stringResource(messageRes ?: R.string.afternote_detail_load_error),
            )
        }
    }
}

/**
 * 삭제 진행([com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState.Success.isDeleting])
 * 동안 상세 화면 위에 겹쳐 그리는 오버레이.
 *
 * 반투명 스크림 + 중앙 진행 인디케이터. 중복 delete 호출은 ViewModel 이 이미 가드하므로
 * 여기서는 시각 표시와 입력 차단만 담당한다 (indication 없는 clickable 로 하위 터치 소비).
 *
 * "하위 터치 소비": `fillMaxSize` 로 화면 전체를 덮은 `clickable` 이 pointer 이벤트를 먹어
 * 아래 상세 화면으로 전파되지 않게 한다 → 삭제 중 하위 UI 조작 차단. `onClick` 은 빈 람다로,
 * 목적은 "클릭 동작"이 아니라 "터치를 삼키는 투명 벽"이다. 눌리는 버튼이 아니므로 `indication = null`
 * 로 눌림 시각 피드백(ripple)을 끄고, 이 오버로드가 요구하는 `interactionSource` 는 remember 로
 * 안정 인스턴스를 준다. (clickable 의 이벤트 소비 동작·indication 파라미터:
 * https://developer.android.com/reference/kotlin/androidx/compose/foundation/package-summary)
 */
@Composable
internal fun DeleteInProgressOverlay() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AfternoteDesign.colors.black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 삭제 실패 메시지를 [snackbarHostState] Snackbar 로 표출하는 [ObserveDeleteResult] 용 `onDeleteFailed` 핸들러.
 *
 * [AfternoteDetailDeleteResult.Failed] 계약대로 `rawMessage`(서버 메시지 등)를 우선 사용하고,
 * 비어 있으면 `messageRes` (둘 다 없으면 [R.string.afternote_detail_delete_failed] 폴백).
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
internal fun rememberDeleteFailedHandler(snackbarHostState: SnackbarHostState): (rawMessage: String?, messageRes: Int?) -> Unit {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    return remember(snackbarHostState, resources, scope) {
        { rawMessage, messageRes ->
            val message =
                rawMessage?.takeIf { it.isNotBlank() }
                    ?: resources.getString(messageRes ?: R.string.afternote_detail_delete_failed)
            scope.launch {
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }
}

/**
 * 상세 화면 삭제 결과 ([AfternoteDetailDeleteResult]) 공용 처리 헬퍼.
 *
 * UiState 의 nullable 신호([com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailUiState.Success.deleteResult])를
 * [LaunchedEffect] 로 감지해
 * - [AfternoteDetailDeleteResult.Succeeded] → [onDeleteSucceeded] (보통 호출처가 pop 콜백을 전달)
 * - [AfternoteDetailDeleteResult.Failed] → [onDeleteFailed] (에러 UI는 화면별 Snackbar/Dialog 책임.
 *   보통 [rememberDeleteFailedHandler] 를 전달한다. 무음 삼킴 방지를 위해 필수 파라미터.)
 *
 * 처리 후 [onConsumed] 콜백으로 ViewModel 의 [com.afternote.feature.afternote.presentation.author.detail.AfternoteDetailViewModel.onDeleteResultConsumed]
 * 호출 → state reset (재합성 시 중복 처리 방지).
 */
@Composable
internal fun ObserveDeleteResult(
    deleteResult: AfternoteDetailDeleteResult?,
    onConsumed: () -> Unit,
    onDeleteSucceeded: () -> Unit,
    onDeleteFailed: (rawMessage: String?, messageRes: Int?) -> Unit,
) {
    LaunchedEffect(deleteResult) {
        when (deleteResult) {
            is AfternoteDetailDeleteResult.Succeeded -> {
                onDeleteSucceeded()
                onConsumed()
            }

            is AfternoteDetailDeleteResult.Failed -> {
                onDeleteFailed(deleteResult.rawMessage, deleteResult.messageRes)
                onConsumed()
            }

            null -> {}
        }
    }
}

@Composable
internal fun AfternoteDetailNavigation(
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
) {
    val route = backStackEntry.toRoute<AfternoteRoute.DetailRoute>()
    if (route.itemId.isBlank()) {
        DesignPendingDetailContent(onBackClick = onBack)
    } else {
        AccountDetailRoute(
            onBack = onBack,
            onNavigateToEditor = onNavigateToEditor,
        )
    }
}

@Composable
internal fun AfternoteGalleryDetailNavigation(
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
) {
    GalleryDetailRoute(
        onBack = onBack,
        onNavigateToEditor = onNavigateToEditor,
    )
}

@Preview(showBackground = true)
@Composable
private fun DeleteInProgressOverlayPreview() {
    AfternoteTheme {
        DeleteInProgressOverlay()
    }
}

@Composable
internal fun AfternoteMemorialDetailNavigation(
    onBack: () -> Unit,
    onNavigateToEditor: (itemId: String) -> Unit,
) {
    MemorialDetailRoute(
        onBack = onBack,
        onNavigateToEditor = onNavigateToEditor,
    )
}

@Preview(showBackground = true)
@Composable
private fun DetailLoadErrorContentPreview() {
    AfternoteTheme {
        DetailLoadErrorContent(
            rawMessage = "Error occurred while loading detail data",
            messageRes = null,
            onBackClick = {},
        )
    }
}
