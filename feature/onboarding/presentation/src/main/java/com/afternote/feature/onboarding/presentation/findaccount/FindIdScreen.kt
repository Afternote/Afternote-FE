package com.afternote.feature.onboarding.presentation.findaccount

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.feature.onboarding.presentation.snackbarMessage
import com.afternote.feature.onboarding.presentation.toDisplay
import kotlinx.coroutines.launch

/**
 * 아이디 찾기 1단계 — 이메일 인증. stateful 층.
 *
 * ViewModel 은 `Route.Onboarding` 그래프 스코프 공유라 여기서 만들지 않고 받는다
 * (결과 화면·비밀번호 찾기가 같은 인스턴스를 이어받는다).
 *
 * 인증번호 무효는 시안상 인라인 문구라 [FindIdUiState.failure] 로 화면이 직접 그리고,
 * 그 밖의 실패([FindIdUiState.failure])만 스낵바로 나른다.
 */
@Composable
internal fun FindIdScreen(
    viewModel: FindIdViewModel,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveSignal(
        // VM 이 UiText 로 폴백까지 확정해 두므로 빈 문구가 도달하지 않는다.
        signal =
            state.failure
                .toDisplay()
                .snackbarMessage
                ?.asString(),
        consumed = FindIdIntent.ConsumeError,
        onIntent = viewModel::onIntent,
    ) { message ->
        // 소비가 곧바로 신호를 되돌려 effect 를 재시작시키므로, 표출은 effect 밖 스코프에 맡긴다.
        scope.launch {
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    FindIdContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onNextClick = onNextClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
