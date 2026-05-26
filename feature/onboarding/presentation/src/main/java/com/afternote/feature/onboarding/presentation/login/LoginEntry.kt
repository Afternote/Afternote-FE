package com.afternote.feature.onboarding.presentation.login

import android.app.Activity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.findActivity
import com.afternote.feature.onboarding.presentation.BuildConfig
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.login.social.UserCancelledAuthException
import com.afternote.feature.onboarding.presentation.login.social.requestGoogleIdToken
import com.afternote.feature.onboarding.presentation.login.social.requestKakaoAccessToken
import kotlinx.coroutines.launch

/**
 * 로그인 Entry.
 *
 * ViewModel 주입·이벤트 수집을 전담하고, Screen에 순수 상태만 전달합니다.
 * 소셜 로그인 SDK(카카오) 및 Credential Manager(구글)는 Activity/Context에
 * 강하게 의존하므로 UI 레이어(Entry)에서 처리한 뒤 순수 토큰 문자열만
 * ViewModel로 전달하여 ViewModel과 Data 레이어의 플랫폼 독립성을 보장합니다.
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val loginFailedMessage = stringResource(R.string.login_failed)
    val kakaoFailedMessage = stringResource(R.string.login_kakao_failed)
    val googleFailedMessage = stringResource(R.string.login_google_failed)
    val googleNoCredentialsMessage = stringResource(R.string.login_google_no_credentials)
    val screenUnavailableMessage = stringResource(R.string.login_screen_unavailable)

    val showErrorSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    val withClearFocus: (() -> Unit) -> Unit = { action ->
        focusManager.clearFocus()
        action()
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
            viewModel.onLoggedInConsumed()
        }
    }
    val pendingErrorMessage = uiState.errorMessage
    LaunchedEffect(pendingErrorMessage) {
        if (pendingErrorMessage != null) {
            showErrorSnackbar(pendingErrorMessage.ifBlank { loginFailedMessage })
            viewModel.onErrorConsumed()
        }
    }

    LoginScreen(
        initialEmail = uiState.email,
        initialPassword = uiState.password,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onLoginClick = { withClearFocus { viewModel.loginWithEmail() } },
        onSignUpClick = { withClearFocus { onSignUpClick() } },
        onKakaoLoginClick = {
            withClearFocus {
                val activity = context.findActivity<Activity>()
                if (activity != null) {
                    coroutineScope.launch {
                        requestKakaoAccessToken(activity)
                            .onSuccess { oauthToken ->
                                viewModel.loginWithKakao(oauthToken)
                            }.onFailure { exception ->
                                if (exception is UserCancelledAuthException) return@onFailure
                                showErrorSnackbar(exception.message ?: kakaoFailedMessage)
                            }
                    }
                } else {
                    showErrorSnackbar(screenUnavailableMessage)
                }
            }
        },
        onGoogleLoginClick = {
            withClearFocus {
                coroutineScope.launch {
                    requestGoogleIdToken(
                        context = context,
                        credentialManager = credentialManager,
                        serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                    ).onSuccess { idToken ->
                        viewModel.loginWithGoogle(idToken)
                    }.onFailure { exception ->
                        val message =
                            when (exception) {
                                is UserCancelledAuthException -> return@onFailure
                                is NoCredentialException -> googleNoCredentialsMessage
                                else -> exception.message ?: googleFailedMessage
                            }
                        showErrorSnackbar(message)
                    }
                }
            }
        },
        onBackClick = { withClearFocus { onBackClick() } },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        isLoading = uiState.isLoading,
    )
}
