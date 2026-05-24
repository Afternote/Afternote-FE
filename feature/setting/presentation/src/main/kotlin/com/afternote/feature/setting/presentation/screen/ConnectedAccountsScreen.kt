package com.afternote.feature.setting.presentation.screen

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.findActivity
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.BuildConfig
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.SocialAccountRow
import com.afternote.feature.setting.presentation.social.UserCancelledAuthException
import com.afternote.feature.setting.presentation.social.requestGoogleIdToken
import com.afternote.feature.setting.presentation.social.requestKakaoAccessToken
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
                            if (activity != null) {
                                requestKakaoAccessToken(activity)
                                    .onSuccess { token -> viewModel.link("kakao", token) }
                                    .onFailure { e ->
                                        if (e !is UserCancelledAuthException) {
                                            viewModel.link("kakao", "") // error state 전달용 빈 호출 방지 — 아래 TODO로 대체 가능
                                        }
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
                                    if (e !is UserCancelledAuthException) {
                                        viewModel.notifyLinkError("Google 계정 연결에 실패했습니다.")
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

@Preview(showBackground = true)
@Composable
private fun ConnectedAccountScreenPrev() {
    ConnectedAccountsScreen(onBack = {})
}
