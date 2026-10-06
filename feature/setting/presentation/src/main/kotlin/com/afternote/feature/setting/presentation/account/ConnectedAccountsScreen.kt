package com.afternote.feature.setting.presentation.account

import android.app.Activity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.CredentialManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.common.auth.requestKakaoAccessToken
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.feature.setting.presentation.BuildConfig
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.account.social.KakaoAuthResult
import com.afternote.feature.setting.presentation.account.social.requestGoogleIdToken
import com.afternote.feature.setting.presentation.account.social.toKakaoAuthResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
internal fun ConnectedAccountsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectedAccountsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val kakaoAccountLinkFailedMessage = stringResource(R.string.setting_kakao_account_link_failed)
    val googleAccountLinkFailedMessage = stringResource(R.string.setting_google_account_link_failed)
    // 소비가 ObserveSignal 의 effect 를 다시 시작시켜도 인증·스낵바는 끝까지 간다. 예전 이벤트 수집처럼
    // 요청을 하나씩 처리하도록 화면 코루틴에서 잠금으로 줄을 세운다.
    val linkScope = rememberCoroutineScope()
    val linkMutex = remember { Mutex() }

    uiState.pendingLinkProvider?.let { pendingProvider ->
        ObserveSignal(
            signal = pendingProvider,
            consumed = ConnectedAccountsIntent.ConsumeLinkRequest(pendingProvider),
            onIntent = viewModel::onIntent,
        ) { provider ->
            linkScope.launch {
                linkMutex.withLock {
                    when (provider) {
                        "kakao" -> {
                            val activity = context.findActivity<Activity>()
                            val authResult =
                                activity
                                    ?.let { requestKakaoAccessToken(it).toKakaoAuthResult() }
                                    ?: KakaoAuthResult.Failure
                            when (authResult) {
                                is KakaoAuthResult.Success -> {
                                    viewModel.onIntent(ConnectedAccountsIntent.Link("kakao", authResult.accessToken))
                                }

                                KakaoAuthResult.Cancelled -> {}

                                KakaoAuthResult.Failure -> {
                                    snackbarHostState.showSnackbar(kakaoAccountLinkFailedMessage)
                                }
                            }
                        }

                        "google" -> {
                            requestGoogleIdToken(
                                context = context,
                                credentialManager = credentialManager,
                                serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                            ).onSuccess { token ->
                                viewModel.onIntent(ConnectedAccountsIntent.Link("google", token))
                            }.onFailure { e ->
                                if (e !is CoreAuthFailure.UserCancelledAuth) {
                                    snackbarHostState.showSnackbar(googleAccountLinkFailedMessage)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.pendingError?.let { message ->
        ObserveSignal(message, ConnectedAccountsIntent.ConsumeError(message), viewModel::onIntent) {
            linkScope.launch { snackbarHostState.showSnackbar(it) }
        }
    }

    ConnectedAccountsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRetry = { viewModel.onIntent(ConnectedAccountsIntent.RetryLoad) },
        onToggle = { provider, enabled -> viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider, enabled)) },
        modifier = modifier,
    )
}
