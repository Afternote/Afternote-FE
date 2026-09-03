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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.ui.asString
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.mvi.ObserveFlag
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.feature.onboarding.presentation.BuildConfig
import com.afternote.feature.onboarding.presentation.R
import com.afternote.feature.onboarding.presentation.displayMessageResOrFallback
import com.afternote.feature.onboarding.presentation.login.social.requestGoogleIdToken
import com.afternote.feature.onboarding.presentation.login.social.requestKakaoAccessToken
import com.afternote.feature.onboarding.presentation.reporting.AuthProvider
import kotlinx.coroutines.launch

/**
 * 로그인 화면 — stateful 층.
 *
 * ViewModel 주입과 일회성 신호 소비를 전담하고, [LoginContent] 에는 상태와 `onIntent` 만 넘긴다.
 * 소셜 로그인 SDK(카카오)와 Credential Manager(구글)는 Activity·Context 에 강하게 의존하므로
 * 여기서 토큰을 받아낸 뒤 문자열만 Intent 로 보낸다 — ViewModel·data 계층의 플랫폼 독립성을
 * 그대로 유지한다.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNewUserOnboarding: () -> Unit,
    onSignUpClick: () -> Unit,
    onFindAccountClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    ObserveFlag(
        raised = uiState.isLoggedIn,
        consumed = LoginIntent.ConsumeLoggedIn,
        onIntent = viewModel::onIntent,
        onRaised = onLoginSuccess,
    )
    ObserveFlag(
        raised = uiState.shouldStartOnboarding,
        consumed = LoginIntent.ConsumeOnboardingStart,
        onIntent = viewModel::onIntent,
        onRaised = onNewUserOnboarding,
    )
    ObserveSignal(
        signal = uiState.errorMessage?.asString(),
        consumed = LoginIntent.ConsumeError,
        onIntent = viewModel::onIntent,
        // showErrorSnackbar 가 별도 스코프에 launch 하므로 소비 직후의 재시작이 표출을 끊지 않는다.
        onSignal = showErrorSnackbar,
    )

    LoginContent(
        state = uiState,
        onIntent = viewModel::onIntent,
        onSignUpClick = onSignUpClick,
        onFindAccountClick = onFindAccountClick,
        onKakaoLoginClick = {
            val activity = context.findActivity<Activity>()
            if (activity != null) {
                coroutineScope.launch {
                    requestKakaoAccessToken(activity)
                        .onSuccess { oauthToken ->
                            viewModel.onIntent(LoginIntent.SubmitKakaoLogin(oauthToken))
                        }.onFailure { exception ->
                            // 카카오 동의 화면·계정 로그인 창을 사용자가 닫은 경우(ClientErrorCause.Cancelled).
                            // 장애가 아니라 정상적인 이탈이므로 리포팅하지 않는다.
                            if (exception is CoreAuthFailure.UserCancelledAuth) return@onFailure
                            viewModel.onIntent(
                                LoginIntent.ReportSocialTokenFailure(AuthProvider.KAKAO, exception),
                            )
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
        },
        onGoogleLoginClick = {
            coroutineScope.launch {
                requestGoogleIdToken(
                    context = context,
                    credentialManager = credentialManager,
                    serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                ).onSuccess { idToken ->
                    viewModel.onIntent(LoginIntent.SubmitGoogleLogin(idToken))
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
                    viewModel.onIntent(
                        LoginIntent.ReportSocialTokenFailure(AuthProvider.GOOGLE, exception),
                    )
                    showErrorSnackbar(message)
                }
            }
        },
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
