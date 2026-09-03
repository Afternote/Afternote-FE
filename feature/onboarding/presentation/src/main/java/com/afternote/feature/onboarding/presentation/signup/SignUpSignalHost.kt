package com.afternote.feature.onboarding.presentation.signup

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.afternote.core.ui.asString
import com.afternote.core.ui.mvi.ObserveFlag
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.feature.onboarding.presentation.R
import kotlinx.coroutines.launch

/**
 * SignUp Step 1~4 · Profile 이 공유하는 스낵바 호스트와 공통 신호 소비.
 *
 * Step 화면마다 같은 `LaunchedEffect` 를 다시 쓰면 소비 규약이 화면 수만큼 갈린다 —
 * 신호를 읽고 `Intent.ConsumeXxx` 를 되쏘는 일은 [ObserveSignal]·[ObserveFlag] 하나로 모은다.
 *
 * 화면 고유 신호(Step 1 의 [SignUpUiState.shouldNavigateToResidentNumber], Profile 의
 * [SignUpUiState.isSignedUp])는 그 화면이 직접 소비한다.
 */
@Composable
internal fun rememberSignUpSnackbarHost(
    state: SignUpUiState,
    onIntent: (SignUpIntent) -> Unit,
): SnackbarHostState {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val nameRequiredMessage = stringResource(R.string.onboarding_signup_name_required)

    ObserveFlag(
        raised = state.isNameRequired,
        consumed = SignUpIntent.ConsumeNameRequired,
        onIntent = onIntent,
    ) {
        // 소비가 곧바로 신호를 되돌려 effect 를 재시작시키므로, 표출은 effect 밖 스코프에 맡긴다.
        scope.launch {
            snackbarHostState.showSnackbar(message = nameRequiredMessage, duration = SnackbarDuration.Short)
        }
    }

    ObserveSignal(
        // VM 이 UiText 로 폴백까지 확정해 두므로 빈 문구가 도달하지 않는다.
        signal = state.errorMessage?.asString(),
        consumed = SignUpIntent.ConsumeError,
        onIntent = onIntent,
    ) { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    return snackbarHostState
}
