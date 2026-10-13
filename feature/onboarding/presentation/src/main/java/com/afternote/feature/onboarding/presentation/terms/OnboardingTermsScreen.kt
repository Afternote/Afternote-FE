package com.afternote.feature.onboarding.presentation.terms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import com.afternote.feature.onboarding.presentation.signup.rememberSignUpSnackbarHost

internal enum class TermsType {
    SERVICE,
    PRIVACY,
    MARKETING,
}

@Immutable
internal data class TermsState(
    val isTermsAgreed: Boolean = false,
    val isPrivacyAgreed: Boolean = false,
    val isMarketingAgreed: Boolean = false,
) {
    val isAllAgreed: Boolean get() = isTermsAgreed && isPrivacyAgreed && isMarketingAgreed
}

/**
 * 약관 동의(Step 4) — stateful 층.
 *
 * 약관 상세·다음 단계 이동은 네비게이션이라 콜백으로 남는다.
 */
@Composable
internal fun OnboardingTermsScreen(
    viewModel: SignUpViewModel,
    onViewTermsClick: (TermsType) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OnboardingTermsContent(
        state = state,
        onIntent = viewModel::onIntent,
        snackbarHostState = rememberSignUpSnackbarHost(state, viewModel::onIntent),
        onViewTermsClick = onViewTermsClick,
        onNextClick = onNextClick,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
