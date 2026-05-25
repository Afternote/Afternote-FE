package com.afternote.feature.onboarding.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel

/**
 * 프로필 설정 Entry.
 *
 * Graph-scoped [SignUpViewModel] 의 [SignUpUiState] 단발성 신호 (signUpSucceeded ·
 * errorMessage · nameRequired) 를 LaunchedEffect 로 소비. 소비 후 VM 의 `onXxxConsumed()`
 * 호출로 reset.
 */
@Composable
fun OnboardingProfileEntry(
    viewModel: SignUpViewModel,
    onOnboardingComplete: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profileImageUri by viewModel.profileImageUri.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val signupFailedMessage = stringResource(R.string.signup_failed)
    val nameRequiredMessage = stringResource(R.string.signup_name_required)

    LaunchedEffect(uiState.signUpSucceeded) {
        if (uiState.signUpSucceeded) {
            onOnboardingComplete()
            viewModel.onSignUpSucceededConsumed()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message.ifBlank { signupFailedMessage },
                duration = SnackbarDuration.Short,
            )
            viewModel.onErrorMessageConsumed()
        }
    }

    LaunchedEffect(uiState.nameRequired) {
        if (uiState.nameRequired) {
            snackbarHostState.showSnackbar(
                message = nameRequiredMessage,
                duration = SnackbarDuration.Short,
            )
            viewModel.onNameRequiredConsumed()
        }
    }

    OnboardingProfileScreen(
        initialName = uiState.name,
        displayImageUri = profileImageUri,
        snackbarHostState = snackbarHostState,
        onNameChange = viewModel::updateName,
        onProfileImagePick = viewModel::onProfileImagePicked,
        onCompleteClick = viewModel::submitSignUp,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
