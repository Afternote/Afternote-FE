package com.afternote.feature.setting.presentation.screen

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.SocialAccountRow
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsUiState

/**
 * 연결된 계정 화면의 상태 없는 본문.
 *
 * ViewModel·소셜 인증 연동은 [ConnectedAccountsScreen] 이 들고, 이 함수는 넘겨받은 상태만 그린다.
 */
@Composable
internal fun ConnectedAccountsContent(
    uiState: ConnectedAccountsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onToggle: (provider: String, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    onToggle = { enabled -> onToggle(account.provider, enabled) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
