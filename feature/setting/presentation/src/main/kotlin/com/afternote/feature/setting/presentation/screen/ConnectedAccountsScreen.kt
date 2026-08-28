package com.afternote.feature.setting.presentation.screen

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.domain.error.CoreAuthFailure
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.BuildConfig
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.SettingLoadErrorContent
import com.afternote.feature.setting.presentation.component.SocialAccountRow
import com.afternote.feature.setting.presentation.social.KakaoAuthResult
import com.afternote.feature.setting.presentation.social.requestGoogleIdToken
import com.afternote.feature.setting.presentation.social.requestKakaoAccessToken
import com.afternote.feature.setting.presentation.social.toKakaoAuthResult
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsEvent
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsViewModel

@Composable
fun ConnectedAccountsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConnectedAccountsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val kakaoAccountLinkFailedMessage = stringResource(R.string.kakao_account_link_failed)
    val googleAccountLinkFailedMessage = stringResource(R.string.google_account_link_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConnectedAccountsEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
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
                                    viewModel.link("kakao", authResult.accessToken)
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
                            ).onSuccess { token -> viewModel.link("google", token) }
                                .onFailure { e ->
                                    if (e !is CoreAuthFailure.UserCancelledAuth) {
                                        viewModel.notifyLinkError(
                                            googleAccountLinkFailedMessage,
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopBar(
                title = "연결된 계정",
                onBackClick = onBack,
            )
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                SettingLoadErrorContent(
                    message = stringResource(R.string.setting_connected_accounts_load_error),
                    onRetry = viewModel::retryLoadConnectedAccounts,
                    modifier = Modifier.padding(paddingValues),
                )
            }

            else -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.sns_login_section_title),
                        style = AfternoteDesign.typography.bodyLargeR,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.sns_login_section_desc),
                        style = AfternoteDesign.typography.bodySmallR,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    uiState.accounts.forEach { account ->
                        SocialAccountRow(
                            account = account,
                            onToggle = { enabled -> viewModel.onToggle(account.provider, enabled) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectedAccountScreenPrev() {
    ConnectedAccountsScreen(onBack = {})
}
