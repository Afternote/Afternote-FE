package com.afternote.feature.onboarding.presentation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.asString
import com.afternote.feature.onboarding.presentation.signup.SignUpUiState
import com.afternote.feature.onboarding.presentation.signup.SignUpViewModel
import kotlinx.coroutines.launch

/**
 * 프로필 설정 Entry.
 *
 * Graph-scoped [SignUpViewModel] 의 [SignUpUiState] 단발성 신호 (isSignedUp ·
 * errorMessage · isNameRequired) 를 LaunchedEffect 로 소비. 소비 후 VM 의 `onXxxConsumed()`
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val nameRequiredMessage = stringResource(R.string.signup_name_required)

    val showSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(uiState.isSignedUp) {
        if (uiState.isSignedUp) {
            onOnboardingComplete()
            viewModel.onSignedUpConsumed()
        }
    }
    LaunchedEffect(uiState.isNameRequired) {
        if (uiState.isNameRequired) {
            showSnackbar(nameRequiredMessage)
            viewModel.onNameRequiredConsumed()
        }
    }
    // VM 이 UiText 로 폴백까지 확정해 두므로 빈 문구가 도달하지 않는다.
    val pendingErrorMessage = uiState.errorMessage?.asString()
    LaunchedEffect(pendingErrorMessage) {
        if (pendingErrorMessage != null) {
            showSnackbar(pendingErrorMessage)
            viewModel.onErrorConsumed()
        }
    }

    // VM 은 Android Framework 의존 제거를 위해 String 으로 보관. UI 레이어에서 Uri 와 변환.
    OnboardingProfileScreen(
        initialName = uiState.name,
        displayImageUri = uiState.profileImageUri?.toUri(),
        snackbarHostState = snackbarHostState,
        onNameChange = viewModel::updateName,
        onProfileImagePick = { uri -> viewModel.onProfileImagePicked(uri?.toString()) },
        onCompleteClick = viewModel::submitSignUp,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}
