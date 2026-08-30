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
import com.afternote.feature.onboarding.presentation.login.passkey.requestPasskeyAssertion
import com.afternote.feature.onboarding.presentation.login.social.requestGoogleIdToken
import com.afternote.feature.onboarding.presentation.login.social.requestKakaoAccessToken
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import kotlinx.coroutines.launch

/**
 * 로그인 Entry.
 *
 * ViewModel 주입·이벤트 수집을 전담하고, Screen에 순수 상태만 전달합니다.
 * 소셜 로그인 SDK(카카오) 및 Credential Manager(구글·패스키)는 Activity/Context에
 * 강하게 의존하므로 UI 레이어(Entry)에서 처리한 뒤 순수 토큰 문자열만
 * ViewModel로 전달하여 ViewModel과 Data 레이어의 플랫폼 독립성을 보장합니다.
 *
 * 패스키(#764)는 전용 버튼이 없습니다 — [공식 UX 권고](https://developer.android.com/design/ui/mobile/guides/patterns/passkeys)
 * 가 수단별 버튼 대신 통합 선택기를 쓰라고 해서, 진입점은 화면 진입 자체입니다. 등록된 패스키가
 * 없으면 `NoCredentialException` 을 조용히 삼키고 기존 로그인 폼이 그대로 남습니다.
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

    // 화면 진입 시 패스키 인증 옵션을 미리 받아 둔다. 중복 시도 차단은 ViewModel 이 맡는다 —
    // 구성 변경으로 이 LaunchedEffect 가 다시 돌아도 시스템 선택기가 두 번 뜨지 않는다.
    LaunchedEffect(Unit) {
        viewModel.startPasskeyLogin()
    }
    // 옵션이 도착하면 시스템 통합 선택기를 띄운다. Credential Manager 는 Activity 를 쥐고 있어야
    // 해서 ViewModel 이 직접 부를 수 없고, 이 자리(컴포지션 수명)에서만 부를 수 있다.
    LaunchedEffect(uiState.passkeyRequestJson) {
        val requestJson = uiState.passkeyRequestJson ?: return@LaunchedEffect
        viewModel.onPasskeyRequestConsumed()
        requestPasskeyAssertion(
            context = context,
            credentialManager = credentialManager,
            requestJson = requestJson,
        ).onSuccess { assertionJson ->
            viewModel.loginWithPasskey(assertionJson)
        }.onFailure { exception ->
            // 사용자가 요청한 적 없는 자동 시도다. "이 기기에 쓸 패스키가 없다"
            // (NoCredentialException) 와 "시트를 닫았다" 는 실패가 아니라 예정된 결말이라
            // 화면에도 콘솔에도 남기지 않는다 — 구글 로그인 쪽이 NoCredentialException 을
            // 안내·계측 대상으로 삼는 것과 갈리는 지점이고, 이유는 그쪽이 사용자가 버튼을
            // 눌러 시작한 흐름이라 "왜 아무 일도 안 일어났는지" 를 설명해야 하기 때문이다.
            if (exception is NoCredentialException) return@onFailure
            if (exception is CoreAuthFailure.UserCancelledAuth) return@onFailure
            viewModel.onPasskeyAssertionFailed(exception)
        }
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
