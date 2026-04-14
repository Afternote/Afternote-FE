package com.afternote.feature.onboarding.presentation.login

import android.app.Activity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.credentials.CredentialManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.ObserveAsEvents
import com.afternote.core.ui.findActivity
import com.afternote.feature.onboarding.presentation.login.social.UserCancelledAuthException
import com.afternote.feature.onboarding.presentation.login.social.requestGoogleIdToken
import com.afternote.feature.onboarding.presentation.login.social.requestKakaoAccessToken
import kotlinx.coroutines.launch

// TODO: Google Cloud Console Web Client ID. `local.properties` + BuildConfig(`core/startup`의 카카오 키 패턴 참고)
//  또는 DI로 주입하도록 대체 필요. 저장소에 실제 키를 커밋하지 말 것. + 검토
private const val GOOGLE_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID"

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

    ObserveAsEvents(viewModel.eventFlow) { event ->
        when (event) {
            is LoginEvent.LoginSuccess -> onLoginSuccess()
            is LoginEvent.ShowError -> showErrorSnackbar(event.message)
        }
    }

    LoginScreen(
        emailState = viewModel.emailState,
        passwordState = viewModel.passwordState,
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
                                showErrorSnackbar(exception.message ?: "카카오 로그인에 실패했습니다.")
                            }
                    }
                } else {
                    showErrorSnackbar("로그인 화면을 띄울 수 없습니다.")
                }
            }
        },
        onGoogleLoginClick = {
            withClearFocus {
                coroutineScope.launch {
                    requestGoogleIdToken(
                        context = context,
                        credentialManager = credentialManager,
                        serverClientId = GOOGLE_WEB_CLIENT_ID,
                    ).onSuccess { idToken ->
                        viewModel.loginWithGoogle(idToken)
                    }.onFailure { exception ->
                        if (exception is UserCancelledAuthException) return@onFailure
                        showErrorSnackbar(exception.message ?: "구글 로그인에 실패했습니다.")
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
