package com.afternote.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.feature.onboarding.presentation.signup.SignUpEvent
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel

/**
 * 프로필 설정 Entry.
 *
 * Graph-scoped [SignUpViewModel]을 받아 이벤트 수집과 상태 전달을 전담합니다.
 */
@Composable
fun OnboardingProfileEntry(
    viewModel: SignUpViewModel,
    onOnboardingComplete: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profileImageUri by viewModel.profileImageUri.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.eventFlow) { event ->
        when (event) {
            is SignUpEvent.SignUpSuccess -> onOnboardingComplete()
            is SignUpEvent.ShowError -> {
                // TODO: 스낵바 또는 토스트로 에러 표시
            }
        }
    }

    OnboardingProfileScreen(
        nameState = viewModel.nameState,
        displayImageUri = profileImageUri,
        onProfileImagePick = viewModel::onProfileImagePicked,
        onCompleteClick = viewModel::submitSignUp,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
