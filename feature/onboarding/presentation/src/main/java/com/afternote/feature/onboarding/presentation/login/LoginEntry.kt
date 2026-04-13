package com.afternote.feature.onboarding.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.domain.usecase.auth.LoginType
import com.afternote.core.ui.ObserveAsEvents

/**
 * 로그인 Entry.
 *
 * ViewModel 주입·이벤트 수집을 전담하고, Screen에 순수 상태만 전달합니다.
 */
@Composable
fun LoginEntry(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    ObserveAsEvents(viewModel.eventFlow) { event ->
        focusManager.clearFocus()
        when (event) {
            is LoginEvent.LoginSuccess -> onLoginSuccess()
            is LoginEvent.ShowError -> {
                // TODO: 스낵바 또는 토스트로 에러 표시
            }
        }
    }

    LoginScreen(
        emailState = viewModel.emailState,
        passwordState = viewModel.passwordState,
        onLoginClick = viewModel::loginWithEmail,
        onSignUpClick = onSignUpClick,
        onKakaoLoginClick = { viewModel.login(LoginType.Kakao) },
        onGoogleLoginClick = { viewModel.login(LoginType.Google) },
        onBackClick = onBackClick,
        modifier = modifier,
        isLoading = uiState.isLoading,
    )
}
