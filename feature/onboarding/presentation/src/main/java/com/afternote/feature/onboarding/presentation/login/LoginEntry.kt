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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.ui.asString
import com.afternote.core.ui.findActivity
import com.afternote.feature.onboarding.presentation.BuildConfig
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.displayMessageResOrFallback
import com.afternote.feature.onboarding.presentation.login.social.requestGoogleIdToken
import com.afternote.feature.onboarding.presentation.login.social.requestKakaoAccessToken
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
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
    onNewUserOnboarding: () -> Unit,
    onSignUpClick: () -> Unit,
    onFindAccountClick: () -> Unit,
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

    // 코루틴 콜백 안에서 여는 문구는 `LocalResources` 로 조회한다 — `LocalContext.current` 경유
    // `getString` 은 로케일·구성이 바뀌어도 옛 문자열을 물고, lint 가 막는다
    // (LocalContextGetResourceValueCall). 여기 `context` 는 Activity·Credential Manager 용이다.
    val resources = LocalResources.current

    val googleNoCredentialsMessage = stringResource(R.string.onboarding_login_google_no_credentials)
    val screenUnavailableMessage = stringResource(R.string.onboarding_login_screen_unavailable)

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
    LaunchedEffect(uiState.shouldStartOnboarding) {
        if (uiState.shouldStartOnboarding) {
            onNewUserOnboarding()
            viewModel.onOnboardingStartConsumed()
        }
    }
    val pendingErrorMessage = uiState.errorMessage?.asString()
    LaunchedEffect(pendingErrorMessage) {
        if (pendingErrorMessage != null) {
            showErrorSnackbar(pendingErrorMessage)
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
        onFindAccountClick = { withClearFocus { onFindAccountClick() } },
        onKakaoLoginClick = {
            withClearFocus {
                val activity = context.findActivity<Activity>()
                if (activity != null) {
                    coroutineScope.launch {
                        requestKakaoAccessToken(activity)
                            .onSuccess { oauthToken ->
                                viewModel.loginWithKakao(oauthToken)
                            }.onFailure { exception ->
                                // 카카오 동의 화면·계정 로그인 창을 사용자가 닫은 경우(ClientErrorCause.Cancelled).
                                // 장애가 아니라 정상적인 이탈이므로 리포팅하지 않는다.
                                if (exception is CoreAuthFailure.UserCancelledAuth) return@onFailure
                                viewModel.onSocialTokenRequestFailed(AuthProvider.KAKAO, exception)
                                showErrorSnackbar(
                                    resources.getString(
                                        exception.displayMessageResOrFallback(R.string.onboarding_login_kakao_failed),
                                    ),
                                )
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
                                // 계정 선택 시트를 사용자가 닫은 경우(GetCredentialCancellationException,
                                // 내부 타입 TYPE_USER_CANCELED). 정상적인 이탈이라 리포팅하지 않는다.
                                is CoreAuthFailure.UserCancelledAuth -> {
                                    return@onFailure
                                }

                                // 반면 이건 취소가 아니라 "쓸 계정이 없어 로그인 불가"라 리포팅 대상이다 —
                                // 배포본 소셜 로그인 불능을 잡아내려면 이쪽이 신호여야 한다.
                                is NoCredentialException -> {
                                    googleNoCredentialsMessage
                                }

                                else -> {
                                    resources.getString(
                                        exception.displayMessageResOrFallback(R.string.onboarding_login_google_failed),
                                    )
                                }
                            }
                        viewModel.onSocialTokenRequestFailed(AuthProvider.GOOGLE, exception)
                        showErrorSnackbar(message)
                    }
                }
            }
        },
        onBackClick = { withClearFocus { onBackClick() } },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
        isLoading = uiState.isLoading,
        hasCredentialError = uiState.hasCredentialError,
        showNetworkErrorPopup = uiState.showNetworkErrorPopup,
        onRetryLogin = viewModel::retryLogin,
        onNetworkErrorDismiss = viewModel::onNetworkErrorDismissed,
    )
}
