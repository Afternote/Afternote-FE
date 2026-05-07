package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R
import com.afternote.feature.setting.presentation.component.SocialAccountRow
import com.afternote.feature.setting.presentation.viewmodel.ConnectedAccountsViewModel

@Composable
fun ConnectedAccountsScreen(
    onBack: () -> Unit,
    viewModel: ConnectedAccountsViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    Scaffold(
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
            // 섹션 헤더
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

            // 소셜 계정 리스트
            accounts.forEach { account ->
                SocialAccountRow(
                    account = account,
                    onToggle = { enabled ->
                        viewModel.onToggleAccount(account.iconRes, enabled) // provider → iconRes
                    },
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
