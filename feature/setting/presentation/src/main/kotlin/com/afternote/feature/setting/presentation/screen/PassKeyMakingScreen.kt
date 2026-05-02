package com.afternote.feature.setting.presentation.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteButton
import com.afternote.core.ui.button.AfternoteButtonType
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.core.ui.topbar.DetailTopBar
import com.afternote.feature.setting.presentation.R

@Composable
fun PassKeyMakingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(id = R.string.passkey_management_title),
                onBackClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(137.dp))
                Text(
                    text = stringResource(R.string.passkey_fingerprint_guide),
                    style = AfternoteDesign.typography.bodyLargeR,
                    modifier = Modifier.padding(innerPadding),
                )
                Spacer(modifier = Modifier.height(40.dp))
                Image(
                    painterResource(R.drawable.ic_fingerprint),
                    "지문",
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 63.dp),
            ) {
                AfternoteButton(
                    text = "지문 인증하기",
                    onClick = {},
                    type = AfternoteButtonType.Default,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AfternoteButton(
                    text = "비밀번호로 인증하기",
                    onClick = {},
                    type = AfternoteButtonType.Active,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PassKeyMakingScreenPrev() {
    PassKeyMakingScreen(onBackClick = {})
}
