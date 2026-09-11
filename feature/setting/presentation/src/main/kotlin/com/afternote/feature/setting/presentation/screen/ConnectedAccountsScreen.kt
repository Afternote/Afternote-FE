package com.afternote.feature.setting.presentation.screen

import android.app.Activity
import android.content.res.Resources
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.ui.UiText
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.mvi.ObserveSignal
import com.afternote.feature.setting.presentation.BuildConfig
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.social.KakaoAuthResult
import com.afternote.feature.setting.presentation.social.requestGoogleIdToken
import com.afternote.feature.setting.presentation.social.requestKakaoAccessToken
import com.afternote.feature.setting.presentation.social.toKakaoAuthResult
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsEvent
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsIntent
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsViewModel
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
    val resources = LocalResources.current
    val kakaoAccountLinkFailedMessage = stringResource(R.string.kakao_account_link_failed)
    val googleAccountLinkFailedMessage = stringResource(R.string.google_account_link_failed)

    val scope = rememberCoroutineScope()
    val eventMutex = remember { Mutex() }
    val pendingEvent = uiState.pendingEvent
    if (pendingEvent != null) {
        ObserveSignal(
            signal = pendingEvent,
            consumed = ConnectedAccountsIntent.ConsumeEvent(pendingEvent),
            onIntent = viewModel::onIntent,
        ) { event ->
            // 소비로 effect가 재시작되어도 인증·스낵바 작업은 계속한다. 기존 이벤트 수집처럼 순차 처리한다.
            scope.launch {
                eventMutex.withLock {
                    when (event) {
                        is ConnectedAccountsEvent.ShowError -> {
                            snackbarHostState.showSnackbar(event.message.resolve(resources))
                        }

                        is ConnectedAccountsEvent.RequestLink -> {
                            when (event.provider) {
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
                                            snackbarHostState.showSnackbar(
                                                kakaoAccountLinkFailedMessage,
                                            )
                                        }
                                    }
                                }

                                "google" -> {
                                    requestGoogleIdToken(
                                        context = context,
                                        credentialManager = credentialManager,
                                        serverClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                                    ).onSuccess { token -> viewModel.onIntent(ConnectedAccountsIntent.Link("google", token)) }
                                        .onFailure { e ->
                                            if (e !is CoreAuthFailure.UserCancelledAuth) {
                                                viewModel.onIntent(ConnectedAccountsIntent.NotifyLinkError(googleAccountLinkFailedMessage))
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ConnectedAccountsContent(
        uiState = uiState,
        onRetry = { viewModel.onIntent(ConnectedAccountsIntent.RetryLoad) },
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onToggle = { provider, enabled -> viewModel.onIntent(ConnectedAccountsIntent.Toggle(provider, enabled)) },
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun ConnectedAccountScreenPrev() {
    ConnectedAccountsScreen(onBack = {})
}

/** `UiText.asString()` 은 `@Composable` 이라 스낵바 코루틴 안에서는 못 부른다. 그 자리용 Resources 풀이. */
private fun UiText.resolve(resources: Resources): String =
    when (this) {
        is UiText.Resource -> if (args.isEmpty()) resources.getString(resId) else resources.getString(resId, *args.toTypedArray())
        is UiText.Dynamic -> value
        is UiText.DynamicOrResource -> value ?: resources.getString(fallbackResId)
    }
