package com.afternote.feature.onboarding.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.feature.onboarding.presentation.signup.SignUpEvent
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val signupFailedMessage = stringResource(R.string.signup_failed)
    val nameRequiredMessage = stringResource(R.string.signup_name_required)

    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    ObserveAsEvents(viewModel.eventFlow) { event ->
        when (event) {
            is SignUpEvent.SignUpSuccess -> onOnboardingComplete()
            is SignUpEvent.NameRequired -> showSnackbar(nameRequiredMessage)
            is SignUpEvent.ShowError -> showSnackbar(event.message ?: signupFailedMessage)
        }
    }

    OnboardingProfileScreen(
        nameState = viewModel.nameState,
        displayImageUri = profileImageUri,
        snackbarHostState = snackbarHostState,
        onProfileImagePick = viewModel::onProfileImagePicked,
        onCompleteClick = viewModel::submitSignUp,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
