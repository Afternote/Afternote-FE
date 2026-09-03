package com.afternote.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.mvi.ObserveFlag
import com.afternote.feature.onboarding.presentation.signup.SignUpIntent
import com.afternote.feature.onboarding.presentation.signup.SignUpUiState
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import com.afternote.feature.onboarding.presentation.signup.rememberSignUpSnackbarHost

/**
 * 프로필 설정 — stateful 층.
 *
 * 그래프 스코프 [SignUpViewModel] 의 일회성 신호를 소비한다. 이름 미입력·실패 문구는 Step 화면과
 * 같은 관용구([rememberSignUpSnackbarHost])를 쓰고, 완료 신호([SignUpUiState.isSignedUp])만
 * 이 화면이 직접 소비한다.
 */
@Composable
fun OnboardingProfileScreen(
    viewModel: SignUpViewModel,
    onOnboardingComplete: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberSignUpSnackbarHost(state, viewModel::onIntent)

    ObserveFlag(
        raised = state.isSignedUp,
        consumed = SignUpIntent.ConsumeSignedUp,
        onIntent = viewModel::onIntent,
        onRaised = onOnboardingComplete,
    )

    OnboardingProfileContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
